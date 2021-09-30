package cn.jiangzeyin;

import cn.jiangzeyin.common.ApplicationBuilder;
import cn.jiangzeyin.common.EnableCommonBoot;
import cn.jiangzeyin.common.spring.SpringUtil;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.ServletComponentScan;
import org.springframework.http.converter.StringHttpMessageConverter;

import java.nio.charset.Charset;
import java.util.Arrays;

/**
 * Created by jiangzeyin on 2017/9/14.
 *
 * @author jiangzeyin
 */
@SpringBootApplication
@ServletComponentScan
@EnableCommonBoot
public class BootOnLineApplication {

    /**
     * 启动执行
     *
     * @param args 参数
     */
    public static void main(String[] args) throws Exception {
        System.out.println(args == null ? 0 : args.length + "  main 参数" + Arrays.toString(args));
        ApplicationBuilder.createBuilder(BootOnLineApplication.class)
                .addHttpMessageConverter(new StringHttpMessageConverter(Charset.forName("UTF-8")))
                .run(args);
        System.out.println(SpringUtil.getEnvironment().getProperty("spring.application.name"));
    }
}
