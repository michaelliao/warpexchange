#!/usr/bin/env python3
# -*- coding: utf-8 -*-

import json
import time
import hmac
import uuid
import urllib
import base64
import datetime

from urllib import request, parse, error

DEFAULT_HOST = 'localhost:8000'


class ApiClient(object):

    def __init__(self, email, password, host=None, https=False, timeout=5, debug=False):
        ep = f'{email}:{password}'
        self._auth = 'Basic ' + \
            base64.b64encode(ep.encode('utf-8')).decode('utf-8')
        self._host = (host or DEFAULT_HOST).lower()
        self._protocol = 'https' if https else 'http'
        self._timeout = timeout
        self._debug = debug

    def _hostname(self):
        n = self._host.find(':')
        if n > 0:
            return self._host[:n]
        return self._host

    def get(self, path, **params):
        return self._http('GET', path, params, None)

    def post(self, path, obj=None):
        data = json.dumps(obj) if obj is not None else None
        return self._http('POST', path, {}, data)

    def _http(self, method, path, params, data):
        headers = {
            'Authorization': self._auth
        }
        if data:
            data = data.encode('utf-8')
            headers['Content-Type'] = 'application/json'
        else:
            data = None
        url = '%s://%s%s?%s' % (self._protocol, self._host,
                                path, parse.urlencode(params))
        req = request.Request(url, data=data, method=method)
        for k, v in headers.items():
            req.add_header(k, v)
        if self._debug:
            self.debug('%s: %s' % (method, url))
            curl = 'curl -v'
            for k, v in headers.items():
                curl = curl + ' -H \'%s: %s\'' % (k, v)
            if method == 'POST':
                curl = curl + ' -d \'%s\'' % data
            curl = curl + ' \'' + url + '\''
            self.debug(curl)
        try:
            with request.urlopen(req, timeout=self._timeout) as f:
                s = f.read()
                r = json.loads(s.decode('utf-8'),
                               object_hook=lambda d: Dict(**d))
                if self._debug:
                    self.debug('Response:\n' + json.dumps(r))
                return r
        except error.HTTPError as err:
            s = err.read()
            if self._debug:
                self.debug(s)
            return json.loads(s.decode('utf-8'), object_hook=lambda d: Dict(**d))

    def debug(self, s):
        now = datetime.datetime.now().strftime('%H:%m:%S')
        print(f'[{now}] {s}')


class Dict(dict):

    def __init__(self, **kw):
        super().__init__(**kw)

    def __getattr__(self, key):
        try:
            return self[key]
        except KeyError:
            raise AttributeError(r"'Dict' object has no attribute '%s'" % key)

    def __setattr__(self, key, value):
        self[key] = value
