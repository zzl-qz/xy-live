package org.xy.live.framework.redis.starter.config;

import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.SerializationException;

public class IGenericJackson2JsonRedisSerializer extends GenericJackson2JsonRedisSerializer {

    public IGenericJackson2JsonRedisSerializer() {
        super(MapperFactory.newInstance());
    }

    /**
     * 对 String 和 Character 直接存储字节，其他对象使用 JSON 序列化（先转化为json字符串再转成字节数组存储）
     *
     * @param source
     * @return
     * @throws SerializationException
     */
    @Override
    public byte[] serialize(Object source) throws SerializationException {

        if (source != null && ((source instanceof String) || (source instanceof Character))) {
            return source.toString().getBytes();
        }
        return super.serialize(source);
    }
}
