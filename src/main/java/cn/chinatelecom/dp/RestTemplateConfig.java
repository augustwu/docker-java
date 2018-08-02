package cn.chinatelecom.dp;

import com.alibaba.fastjson.serializer.SerializerFeature;
import com.alibaba.fastjson.support.config.FastJsonConfig;
import com.alibaba.fastjson.support.spring.FastJsonHttpMessageConverter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;



public class RestTemplateConfig{



    public RestTemplate createRestTemplate() {
        return new RestTemplate(messageConverters());
    }

    /*将json转换器设置为fastjson, 便于转换L<javaBean>*/
    public List<HttpMessageConverter<?>> messageConverters() {

        List<MediaType> supportMediaTypeList = new ArrayList<>();

        supportMediaTypeList.add(MediaType.APPLICATION_JSON_UTF8);


        FastJsonHttpMessageConverter fastJsonHttpMessageConverter = new FastJsonHttpMessageConverter();
        FastJsonConfig config = new FastJsonConfig();

        config.setSerializerFeatures(
                SerializerFeature.WriteMapNullValue,//保留空的字段
                SerializerFeature.WriteNullStringAsEmpty,//String null -> ""
                SerializerFeature.WriteNullNumberAsZero,//Number null -> 0
                SerializerFeature.WriteNullListAsEmpty,//List null-> []
                SerializerFeature.WriteNullBooleanAsFalse);//Boolean null -> false
        fastJsonHttpMessageConverter.setFastJsonConfig(config);
        fastJsonHttpMessageConverter.setSupportedMediaTypes(supportMediaTypeList);
        fastJsonHttpMessageConverter.setDefaultCharset(Charset.forName("UTF-8"));


        List<HttpMessageConverter<?>> messageConverters = new ArrayList<>();
        messageConverters.add(fastJsonHttpMessageConverter);

        return messageConverters;
    }

}
