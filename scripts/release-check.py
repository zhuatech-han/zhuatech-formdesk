#!/usr/bin/env python3
# Copyright 2026 上海如静知华信息科技有限公司 · https://www.zhuatech.cn/ · 微信 zhuatech / zhuatech2
"""发布前检查公开文件、截图、二维码、许可和凭据泄漏。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。"""
import hashlib, json, re, subprocess
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]


def main():
    files = [
        ROOT / x
        for x in subprocess.check_output(
            ["git", "ls-files"], cwd=ROOT, text=True
        ).splitlines()
    ]
    assert files, "Stage intended source files before running this check"
    prohibited = {".env", ".env.restore", "quality-state.json"}
    for f in files:
        assert f.name not in prohibited and not any(
            x in f.parts
            for x in ["node_modules", "target", "output", "private-backups", ".venv"]
        ), f"Private/generated file tracked: {f.name}"
    for name in ["README.md", "README.en.md"]:
        text = (ROOT / name).read_text()
        assert "[中文](README.md) | [English](README.en.md)" in text
        assert "https://www.zhuatech.cn/" in text
        refs = re.findall(r"!?\[[^\]]*\]\(([^)]+)\)", text) + re.findall(
            r'src="([^"]+)"', text
        )
        for link in refs:
            if not link.startswith(("http:", "https:", "mailto:", "#")):
                assert (ROOT / link).is_file(), f"Missing link: {link}"
    cn = (ROOT / "README.md").read_text()
    en = (ROOT / "README.en.md").read_text()
    license = (ROOT / "LICENSE").read_text()
    assert "未经书面授权不得商用" in cn and "未经书面授权不得商用" in license
    assert "Commercial use requires written authorization" in en
    assert "wechat-zhuatech.png" in cn and "wechat-zhuatech2.png" in cn
    assert (
        "wechat-" not in en
        and "han@zhuatech.cn" in en
        and "jack@zhuatech.cn" in en
        and "https://wa.me/8617521234993" in en
    )
    for name in ["wechat-zhuatech.png", "wechat-zhuatech2.png"]:
        a = ROOT / "docs/images" / name
        b = ROOT / "frontend/public/brand" / name
        assert (
            a in files
            and b in files
            and a.read_bytes().startswith(b"\x89PNG\r\n\x1a\n")
        )
        assert (
            hashlib.sha256(a.read_bytes()).digest()
            == hashlib.sha256(b.read_bytes()).digest()
        )
    for f in (ROOT / "docs/screenshots").glob("*.jpg"):
        assert f in files and f.read_bytes().startswith(
            b"\xff\xd8"
        ), f"Invalid/untracked screenshot: {f.name}"
    assert len(list((ROOT / "docs/screenshots").glob("*.jpg"))) >= 6
    leaked = []
    own = []
    for f in files:
        if f.suffix not in [
            ".java",
            ".js",
            ".vue",
            ".css",
            ".py",
            ".sql",
            ".yml",
            ".yaml",
            ".conf",
            ".md",
            ".xml",
            ".html",
            ".example",
        ] and f.name not in ["Dockerfile", "LICENSE"]:
            continue
        text = f.read_text()
        own.append(f)
        assert "https://www.zhuatech.cn/" in text and (
            "zhuatech2" in text
            or f.name == "README.en.md"
            and "han@zhuatech.cn" in text
        ), f"Missing brand header/contact: {f.name}"
        if re.search(
            r"-----BEGIN (?:RSA |OPENSSH |EC )?PRIVATE KEY-----|gh[pousr]_[A-Za-z0-9]{30,}|AKIA[A-Z0-9]{16}",
            text,
        ):
            leaked.append(f.name)
    private = (
        dict(
            line.split("=", 1)
            for line in (ROOT / ".env").read_text().splitlines()
            if "=" in line and not line.startswith("#")
        )
        if (ROOT / ".env").exists()
        else {}
    )
    values = [
        private[k]
        for k in ["MYSQL_ROOT_PASSWORD", "DATABASE_PASSWORD", "ADMIN_PASSWORD"]
        if private.get(k)
    ]
    state = ROOT / "output/quality-state.json"
    if state.exists():
        b = json.loads(state.read_text())
        values.extend(b[k] for k in ["token", "testPassword"] if b.get(k))
    for f in files:
        for value in values:
            if value.encode() in f.read_bytes():
                leaked.append(f.name)
    assert not leaked, f"Sensitive value detected in {set(leaked)}"
    for name in ["backend/Dockerfile", "compose.yaml"]:
        assert (
            "-DskipTests" not in (ROOT / name).read_text()
            and "-Dmaven.test.skip" not in (ROOT / name).read_text()
        )
    print(
        json.dumps(
            {
                "trackedFiles": len(files),
                "sourceBrandFiles": len(own),
                "screenshots": len(list((ROOT / "docs/screenshots").glob("*.jpg"))),
                "originalQrCopies": "matched",
                "bilingualReadmes": "passed",
                "sensitiveLeaks": len(leaked),
            },
            ensure_ascii=False,
        )
    )


if __name__ == "__main__":
    main()
