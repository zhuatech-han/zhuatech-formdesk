# Copyright 2026 上海如静知华信息科技有限公司 · https://www.zhuatech.cn/ · 微信 zhuatech / zhuatech2
"""仅对专用空库执行真实收集、处理、权限和持久化验收。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。"""
import argparse, base64, hashlib, json, os, secrets
import http_client as hc
from http_client import ROOT, Client, env, field


def main():
    p = argparse.ArgumentParser()
    p.add_argument("--base", default="http://127.0.0.1:8142")
    a = p.parse_args()
    assert a.base.startswith("http://127.0.0.1:"), "QA only runs on localhost"
    private = env()
    admin = Client(a.base)
    profile = admin.login(
        private.get("ADMIN_USERNAME", "admin"), private["ADMIN_PASSWORD"]
    )
    assert len(profile["menus"]) == 7 and all(m["enabled"] for m in profile["menus"])
    assert admin.request("/api/forms")["total"] == 0, "Refuse nonempty instance"
    assert (
        admin.request("/api/submissions")["total"] == 0
    ), "Refuse existing collected data"
    guest = Client(a.base)
    guest.request("/api/admin/users", status=401)
    assert admin.request("/actuator/health")["status"] == "UP"
    options = admin.request("/api/options")
    dept = profile["departmentId"]
    suffix = secrets.token_hex(3)
    pw = "Aa9" + secrets.token_hex(20)

    def account(name, role, d):
        return admin.request(
            "/api/admin/users",
            "POST",
            dict(
                username=name,
                displayName="TEST " + name,
                roleId=role,
                departmentId=d,
                enabled=True,
                password=pw,
            ),
        )

    processor = account("processor-" + suffix, 3, dept)
    viewer = account("viewer-" + suffix, 4, dept)
    otherdept = admin.request(
        "/api/admin/departments",
        "POST",
        dict(name="TEST second organization", zone="UTC", enabled=True),
    )
    other = account("other-" + suffix, 2, otherdept["id"])
    pc = Client(a.base)
    pc.login(processor["username"], pw)
    vc = Client(a.base)
    vc.login(viewer["username"], pw)
    oc = Client(a.base)
    oc.login(other["username"], pw)
    fs = [
        dict(field("name", required=True), label="联系人 / Contact"),
        dict(field("email", "EMAIL", True), label="邮箱 / Email"),
        dict(
            field("service", "SELECT", True),
            label="服务 / Service",
            options=["部署 / Deployment", "定制 / Customization"],
        ),
        dict(
            field("detail", "TEXTAREA", True),
            label="定制需求 / Customization details",
            whenKey="service",
            whenValue="定制 / Customization",
        ),
        dict(field("budget", "NUMBER"), label="预算 / Budget"),
        dict(field("date", "DATE"), label="期望日期 / Target date"),
        dict(field("files", "FILE"), label="需求附件 / Brief attachments"),
    ]
    definition = dict(
        title="TEST 网站项目询盘 / Website enquiry",
        description="说明项目需求和交付时间，负责人会查看并处理。 / Describe your project and target date.",
        consentText="TEST 数据仅用于本次演示验收，验收结束后删除。 / TEST data is deleted after verification.",
        departmentId=dept,
        category="ENQUIRY",
        fields=fs,
    )
    f = admin.request("/api/forms", "POST", definition)
    fid = f["id"]
    key = f["publicKey"]
    guest.request("/api/public/forms/" + key, status=410)
    f = admin.request(
        f"/api/forms/{fid}/publish", "POST", dict(version=f["version"], closesAt="")
    )
    pub = guest.request("/api/public/forms/" + key)
    assert pub["title"] == definition["title"] and "departmentId" not in pub
    begun = guest.request("/api/public/forms/" + key + "/begin", "POST", {})
    token = begun["token"]
    rid = begun["response"]["id"]
    h = {"X-Receipt-Key": token}
    assert len(token) == 43
    guest.request(
        "/api/public/response", headers={"X-Receipt-Key": "x" * 43}, status=404
    )
    answer = dict(
        name="TEST 联系人",
        email="test@example.invalid",
        service="定制 / Customization",
        detail="TEST 官网需求收集与内部系统对接",
        budget="10000",
        date="2026-11-01",
    )
    guest.request(
        "/api/public/response/submit",
        "POST",
        dict(version=1, answers={"name": "TEST"}, consented=True),
        400,
        h,
    )
    s = guest.request(
        "/api/public/response",
        "PUT",
        dict(version=1, answers=answer, consented=False),
        headers=h,
    )
    assert s["status"] == "DRAFT"
    resumed = Client(a.base)
    assert resumed.request("/api/public/response", headers=h)["answers"] == answer
    boundary = "formdesk-" + secrets.token_hex(8)
    data = b"%PDF-1.4 TEST file verification"
    parts = []
    for name, value in [("field", "files"), ("version", str(s["version"]))]:
        parts.append(
            f'--{boundary}\r\nContent-Disposition: form-data; name="{name}"\r\n\r\n{value}\r\n'.encode()
        )
    parts.append(
        f'--{boundary}\r\nContent-Disposition: form-data; name="file"; filename="TEST-brief.pdf"\r\nContent-Type: application/pdf\r\n\r\n'.encode()
        + data
        + f"\r\n--{boundary}--\r\n".encode()
    )
    s = guest.request(
        "/api/public/response/files",
        "POST",
        headers={**h, "Content-Type": "multipart/form-data; boundary=" + boundary},
        raw=b"".join(parts),
    )
    attachment = s["attachments"][0]
    assert (
        guest.request(
            "/api/public/response/files/" + str(attachment["id"]),
            headers=h,
            binary=True,
        )
        == data
    )
    second = guest.request("/api/public/forms/" + key + "/begin", "POST", {})
    guest.request(
        "/api/public/response/files/" + str(attachment["id"]),
        headers={"X-Receipt-Key": second["token"]},
        status=404,
    )
    s = guest.request(
        "/api/public/response/submit",
        "POST",
        dict(version=s["version"], answers=answer, consented=True),
        headers=h,
    )
    assert s["status"] == "SUBMITTED"
    guest.request(
        "/api/public/response/submit",
        "POST",
        dict(version=s["version"], answers=answer, consented=True),
        409,
        h,
    )
    oc.request(f"/api/forms/{fid}", status=403)
    assert oc.request("/api/forms")["total"] == 0
    vc.request("/api/submissions/" + str(rid), status=403)
    pc.request("/api/submissions/" + str(rid), status=403)
    s = admin.request(
        f"/api/submissions/{rid}/process",
        "POST",
        dict(
            version=s["version"],
            action="assign",
            assigneeId=processor["id"],
            note="TEST assignment",
        ),
    )
    assert pc.request("/api/submissions/" + str(rid))["assigneeId"] == processor["id"]
    pc.request("/api/admin/users", status=403)
    pc.request(f"/api/forms/{fid}/export", "POST", {}, 403)
    s = pc.request(
        f"/api/submissions/{rid}/process",
        "POST",
        dict(version=s["version"], action="IN_PROGRESS", note="TEST review"),
    )
    s = pc.request(
        f"/api/submissions/{rid}/process",
        "POST",
        dict(
            version=s["version"],
            action="NEEDS_INFO",
            note="TEST 请补充交付范围 / Add delivery scope",
        ),
    )
    receipt = resumed.request("/api/public/response", headers=h)
    assert receipt["status"] == "NEEDS_INFO" and "events" not in receipt
    answer["detail"] += "；TEST 包含移动端表单"
    s = resumed.request(
        "/api/public/response/submit",
        "POST",
        dict(version=s["version"], answers=answer, consented=True),
        headers=h,
    )
    assert s["status"] == "IN_PROGRESS"
    s = pc.request(
        f"/api/submissions/{rid}/process",
        "POST",
        dict(version=s["version"], action="COMPLETED", note="TEST confirmed"),
    )
    export = admin.request(f"/api/forms/{fid}/export", "POST", {}, binary=True)
    assert "TEST 联系人".encode() in export and "TEST-brief.pdf".encode() in export
    body = {
        **definition,
        "title": "TEST 网站项目询盘 / Website enquiry v2",
        "version": f["version"],
    }
    updated = admin.request(f"/api/forms/{fid}", "PUT", body)
    assert guest.request("/api/public/forms/" + key)["title"] == definition["title"]
    f = admin.request(
        f"/api/forms/{fid}/publish",
        "POST",
        dict(version=updated["version"], closesAt=""),
    )
    assert guest.request("/api/public/forms/" + key)["number"] == 2
    assert resumed.request("/api/public/response", headers=h)["revision"]["number"] == 1
    # Close rejects saves, receipt remains readable. Then republish for browser QA.
    f = admin.request(f"/api/forms/{fid}/close", "POST", dict(version=f["version"]))
    guest.request("/api/public/forms/" + key, status=410)
    guest.request(
        "/api/public/response",
        "PUT",
        dict(version=1, answers={}, consented=False),
        410,
        {"X-Receipt-Key": second["token"]},
    )
    resumed.request("/api/public/response", headers=h)
    f = admin.request(
        f"/api/forms/{fid}/publish", "POST", dict(version=f["version"], closesAt="")
    )
    # Delete a draft and verify receipt revocation and attachment cleanup separately.
    draft = admin.request("/api/submissions/" + str(second["response"]["id"]))
    admin.request(
        "/api/submissions/" + str(draft["id"]) + "/purge",
        "POST",
        dict(version=draft["version"]),
    )
    guest.request(
        "/api/public/response", headers={"X-Receipt-Key": second["token"]}, status=404
    )
    # Leave another genuine in-progress submission for UI inspection.
    b = guest.request("/api/public/forms/" + key + "/begin", "POST", {})
    h2 = {"X-Receipt-Key": b["token"]}
    s2 = guest.request(
        "/api/public/response/submit",
        "POST",
        dict(
            version=1,
            answers=dict(answer, service="部署 / Deployment", detail=""),
            consented=True,
        ),
        headers=h2,
    )
    assert "detail" not in s2["answers"]
    s2 = admin.request(
        "/api/submissions/" + str(s2["id"]) + "/process",
        "POST",
        dict(
            version=s2["version"], action="IN_PROGRESS", note="TEST deployment review"
        ),
    )
    reports = admin.request("/api/reports")
    assert reports["states"]["COMPLETED"] == 1 and reports["states"]["IN_PROGRESS"] == 1
    audit = admin.request("/api/audit")
    assert any(x["action"] == "RESPONSE_EXPORT" for x in audit)
    state = {
        "formId": fid,
        "publicKey": key,
        "responseId": rid,
        "token": token,
        "attachmentId": attachment["id"],
        "attachmentHash": hashlib.sha256(data).hexdigest(),
        "processor": processor["username"],
        "testPassword": pw,
        "completed": admin.request("/api/submissions/" + str(rid)),
        "otherResponseId": s2["id"],
        "otherResponse": admin.request("/api/submissions/" + str(s2["id"])),
        "definition": admin.request("/api/forms/" + str(fid)),
        "reports": reports,
    }
    out = ROOT / "output"
    out.mkdir(exist_ok=True)
    fd = os.open(
        out / "quality-state.json", os.O_WRONLY | os.O_CREAT | os.O_EXCL, 0o600
    )
    with os.fdopen(fd, "w") as f:
        json.dump(state, f, ensure_ascii=False)
    print(
        json.dumps(
            {
                "httpChecks": hc.COUNT,
                "coreWorkflow": "passed",
                "permissions": "passed",
                "versioning": "passed",
                "attachments": "passed",
                "credentialsPrinted": False,
            }
        )
    )


if __name__ == "__main__":
    main()
