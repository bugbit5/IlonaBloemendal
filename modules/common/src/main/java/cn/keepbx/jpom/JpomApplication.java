package cn.keepbx.jpom;

import cn.hutool.core.util.CharsetUtil;
import cn.hutool.system.SystemUtil;
import cn.jiangzeyin.common.ApplicationBuilder;
import cn.jiangzeyin.common.validator.ParameterInterceptor;
import cn.keepbx.jpom.common.JpomApplicationEvent;
import cn.keepbx.jpom.common.Type;
import org.springframework.http.converter.StringHttpMessageConverter;

import java.nio.charset.Charset;

/**
 * Jpom
 *
 * @author jiangzeyin
 * @date 2019/4/16
 */
public class JpomApplication extends ApplicationBuilder {
    /**
     *
     */
    public static final String SYSTEM_ID = "system";

    protected static String[] args;
    /**
     * 应用类型
     */
    private static Type appType;
    private static Charset charset;

    private static Class appClass;

    /**
     * 获取程序命令行参数
     *
     * @return 数组
     */
    public static String[] getArgs() {
        return args;
    }

    public JpomApplication(Type appType, Class<?> appClass, String[] args) throws Exception {
        super(appClass);
        // windows 命令关闭程序
        checkWindows(args);
        JpomApplication.appType = appType;
        JpomApplication.appClass = appClass;
        JpomApplication.args = args;

        addHttpMessageConverter(new StringHttpMessageConverter(CharsetUtil.CHARSET_UTF_8));

        // 参数拦截器
        addInterceptor(ParameterInterceptor.class);
        //
        addApplicationEventClient(new JpomApplicationEvent());
    }

    private void checkWindows(String[] args) throws Exception {
        if (SystemUtil.getOsInfo().isWindows()) {
            new JpomClose().main(args);
        }
    }

    /**
     * 获取当前系统编码
     *
     * @return charset
     */
    public static Charset getCharset() {
        if (charset == null) {
            if (SystemUtil.getOsInfo().isLinux()) {
                charset = CharsetUtil.CHARSET_UTF_8;
            } else if (SystemUtil.getOsInfo().isMac()) {
                charset = CharsetUtil.CHARSET_UTF_8;
            } else {
                charset = CharsetUtil.CHARSET_GBK;
            }
        }
        return charset;
    }

    public static Type getAppType() {
        return appType;
    }

    public static Class getAppClass() {
        if (appClass == null) {
            return JpomApplication.class;
        }
        return appClass;
    }
}