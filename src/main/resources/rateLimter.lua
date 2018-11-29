local key = "rate.limit:" .. KEYS[1] --限流KEY
local limit = tonumber(ARGV[1])        --限流大小
local timeout = tonumber(ARGV[2]) -- 限制时间
local current = tonumber(redis.call('get', key) or "0")
if current + 1 > limit then --如果超出限流大小
    return 0
else  --请求数+1，并设置2秒过期
    redis.call("INCRBY", key,"1")
    redis.call("expire", key, timeout)
    return current + 1
end
