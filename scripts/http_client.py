# Copyright 2026 上海如静知华信息科技有限公司 · https://www.zhuatech.cn/ · 微信 zhuatech / zhuatech2
"""同源真实HTTP验收客户端，不输出凭据。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。"""
import http.cookiejar
import json
import urllib.error
import urllib.request
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
COUNT = 0


def env():
    return dict(
        line.split("=", 1)
        for line in (ROOT / ".env").read_text().splitlines()
        if "=" in line and not line.startswith("#")
    )


class Client:
    def __init__(self, base):
        self.base = base.rstrip("/")
        self.opener = urllib.request.build_opener(
            urllib.request.HTTPCookieProcessor(http.cookiejar.CookieJar())
        )
        self.csrf = None

    def request(
        self,
        path,
        method="GET",
        body=None,
        status=200,
        headers=None,
        binary=False,
        raw=None,
    ):
        global COUNT
        h = dict(headers or {})
        if method not in ["GET", "HEAD"]:
            if not self.csrf:
                self.csrf = self.request("/api/auth/csrf")
            h[self.csrf["header"]] = self.csrf["token"]
        data = raw
        if body is not None:
            data = json.dumps(body).encode()
            h["Content-Type"] = "application/json"
        req = urllib.request.Request(
            self.base + path, data=data, headers=h, method=method
        )
        try:
            with self.opener.open(req, timeout=30) as res:
                code = res.status
                content = res.read()
        except urllib.error.HTTPError as e:
            code = e.code
            content = e.read()
        assert (
            code == status
        ), f'{method} {path.split("?")[0]} expected {status}, got {code}'
        COUNT += 1
        return content if binary else json.loads(content) if content else None

    def login(self, user, password):
        return self.request(
            "/api/auth/login", "POST", {"username": user, "password": password}
        )


def field(key, kind="TEXT", required=False, **kwargs):
    return dict(
        key=key,
        type=kind,
        label=key,
        help="",
        required=required,
        options=[],
        whenKey="",
        whenValue="",
        **kwargs,
    )
