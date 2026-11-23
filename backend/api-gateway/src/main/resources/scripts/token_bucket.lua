-- ==============================================================================
-- APIShield — Redis Token Bucket Rate Limiting Lua Script
-- Atomic verification and increment for burst Rate Limiting (RPS)
--
-- KEYS[1] : Rate limit key (e.g. ratelimit:{apiKeyId}:{epochSecond})
-- ARGV[1] : Max requests allowed per window (RPS limit)
-- ARGV[2] : TTL in seconds for the rate limit bucket (default 2 seconds)
--
-- Returns:
--   1 : Allowed (under rate limit)
--   0 : Rejected (rate limit exceeded)
-- ==============================================================================

local key = KEYS[1]
local limit = tonumber(ARGV[1])
local ttl = tonumber(ARGV[2])

local current = redis.call('INCR', key)

if current == 1 then
    redis.call('EXPIRE', key, ttl)
end

if current > limit then
    return 0
else
    return 1
end
