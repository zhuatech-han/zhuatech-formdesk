# Copyright 2026 上海如静知华信息科技有限公司 · https://www.zhuatech.cn/ · 微信 zhuatech / zhuatech2
"""重启或独立恢复后核对完整记录和文件散列。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。"""
import argparse, hashlib, json
from http_client import Client, ROOT, env

p = argparse.ArgumentParser()
p.add_argument("--base", default="http://127.0.0.1:8142")
a = p.parse_args()
private = env()
state = json.loads((ROOT / "output/quality-state.json").read_text())
client = Client(a.base)
client.login(private.get("ADMIN_USERNAME", "admin"), private["ADMIN_PASSWORD"])
for key, idkey in [("completed", "responseId"), ("otherResponse", "otherResponseId")]:
    assert client.request("/api/submissions/" + str(state[idkey])) == state[key], (
        key + " changed"
    )
assert (
    client.request("/api/forms/" + str(state["formId"])) == state["definition"]
), "form snapshots changed"
assert client.request("/api/reports") == state["reports"], "statistics changed"
assert (
    hashlib.sha256(
        client.request("/api/attachments/" + str(state["attachmentId"]), binary=True)
    ).hexdigest()
    == state["attachmentHash"]
), "attachment changed"
receipt = Client(a.base).request(
    "/api/public/response", headers={"X-Receipt-Key": state["token"]}
)
assert receipt["status"] == "COMPLETED", "private receipt changed"
for record in state.get("extraRecords", []):
    assert (
        client.request("/api/submissions/" + str(record["id"])) == record
    ), "Browser record changed"
for file_id, expected in state.get("attachmentHashes", {}).items():
    assert (
        hashlib.sha256(
            client.request("/api/attachments/" + file_id, binary=True)
        ).hexdigest()
        == expected
    ), "Binary attachment changed"
for kind, expected in state.get("catalogs", {}).items():
    actual = client.request("/api/admin/" + kind)
    if kind == "roles":
        for role in actual:
            role["permissions"] = sorted(role["permissions"])
    assert actual == expected, "Administration catalog changed: " + kind
print(
    "Persistence passed: complete response records, form revisions, statistics, binary file hashes, administration catalogs and private receipt."
)
