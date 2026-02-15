-- tokenBucket.lua
-- ARGV[1] = key
-- ARGV[2] = tokens_per_sec (replenishRate)
-- ARGV[3] = burst_capacity

local key = ARGV[1]
local rate = tonumber(ARGV[2])
local capacity = tonumber(ARGV[3])

-- get current time from server
local now = redis.call('TIME') -- returns table {seconds, microseconds}
local nowMillis = now[1] * 1000 + math.floor(now[2] / 1000)

local token_key = key .. ':tokens'
local ts_key = key .. ':tst'

local tokens = tonumber(redis.call('GET', token_key) or capacity)
local last = tonumber(redis.call('GET', ts_key) or 0)

local elapsed = math.max(0, nowMillis - last)
local add = (elapsed / 1000.0) * rate
tokens = math.min(capacity, tokens + add)

local allowed = 0
if tokens >= 1 then
  tokens = tokens - 1
  allowed = 1
end

redis.call('SET', token_key, tokens)
redis.call('SET', ts_key, nowMillis)
return allowed
