#!/usr/bin/env python3
# -*- coding: utf-8 -*-

import json
import time
import random
import datetime
import argparse

from urllib import request

from sdk import ApiClient

PAUSE_TIME = 1


def main():
    parser = argparse.ArgumentParser(description='trading bot')
    parser.add_argument('--email', required=True, help='specify user email')
    parser.add_argument('--password', required=True,
                        help='specify user password')
    parser.add_argument('--host', default='localhost:8001',
                        help='specify host')
    parser.add_argument('--https', default=False,
                        type=bool, help='use https or not')
    parser.add_argument('--timeout', default='5', type=int,
                        help='timeout in seconds')
    parser.add_argument('--debug', default=True, type=bool,
                        help='output debug information')

    args = parser.parse_args()
    client = ApiClient(args.email, args.password, args.host,
                       args.https, args.timeout, args.debug)

    log('start bot...')
    while True:
        try:
            world_price = get_world_price()
            active_orders = client.get('/api/orders')
            buy_orders = [
                order for order in active_orders if order.direction == 'BUY']
            sell_orders = [
                order for order in active_orders if order.direction == 'SELL']
            if len(buy_orders) > 10:
                buy_orders.sort(key=lambda order: order.price)
                client.post(f'/api/orders/{buy_orders[0].id}/cancel')
                time.sleep(PAUSE_TIME)
            if len(sell_orders) > 10:
                sell_orders.sort(key=lambda order: order.price, reverse=True)
                client.post(f'/api/orders/{sell_orders[0].id}/cancel')
                time.sleep(PAUSE_TIME)

            client.post('/api/orders', dict(price=randomPrice(world_price,
                        'BUY'), quantity=randomQuantity(), direction='BUY'))
            time.sleep(PAUSE_TIME)

            client.post('/api/orders', dict(price=randomPrice(world_price,
                        'SELL'), quantity=randomQuantity(), direction='SELL'))
            time.sleep(PAUSE_TIME)
        except Exception as e:
            log(e)
            time.sleep(PAUSE_TIME)


def randomPrice(base_price, direction):
    if direction == 'BUY':
        return base_price + random.randrange(-2000, 1000) / 100
    else:
        return base_price + random.randrange(-1000, 2000) / 100


def randomQuantity():
    return random.randrange(1, 120) / 100


# keep as (price, timestamp):
WORLD_PRICE = (None, 0)


def get_world_price():
    '''
    get world price.
    '''
    global WORLD_PRICE
    # update price:
    url = 'https://api.blockchain.com/v3/exchange/tickers/BTC-USD'
    log(f'try get world price from {url}...')
    req = request.Request(url)
    with request.urlopen(req, timeout=5) as f:
        s = f.read()
        r = json.loads(s.decode('utf-8'))
        p = r['last_trade_price']
        log(f'got price: {p}')
        WORLD_PRICE = (int(float(p)), time.time())
    return WORLD_PRICE[0]


def log(s):
    now = datetime.datetime.now().strftime('%H:%m:%S')
    print(f'[{now}] {s}')


if __name__ == '__main__':
    main()
