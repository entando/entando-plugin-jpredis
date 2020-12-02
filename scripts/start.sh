#!/bin/sh
docker run -d -p 6380:6379 --name redis_test_entando redis
