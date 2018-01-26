package com.kyleduo.rabbits;

import android.app.Activity;
import android.content.Context;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.util.SparseArray;

import com.kyleduo.rabbits.annotations.TargetInfo;
import com.kyleduo.rabbits.annotations.utils.NameParser;
import com.kyleduo.rabbits.navigator.DefaultNavigatorFactory;
import com.kyleduo.rabbits.navigator.INavigatorFactory;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Rabbit instance can be obtained by {@link com.kyleduo.rabbits.Rabbit#from(Object)} method.
 * Normal usage likes this.
 * <pre>
 * Rabbit.from(activity)
 * 		.to("http://rabbits.kyleduo.com/some/path")
 * 		.start();
 * </pre>
 * <p>
 * Created by kyle on 2016/12/7.
 */

@SuppressWarnings({"WeakerAccess", "unused"})
public class Rabbit {
    private static final String TAG = Rabbit.class.getSimpleName();
    private static final String PACKAGE = "com.kyleduo.rabbits";
    private static final String ROUTER_CLASS = PACKAGE + ".Router";
    private static final String ROUTERS_CLASS = PACKAGE + ".Routers";
    private static final String ROUTERS_FIELD_CLASS = "routers";

    /**
     * URI used in the origin of this navigation.
     */
    public static final String KEY_ORIGIN_URI = "rabbits_origin_uri";
    /**
     * URI used for latest Navigator. If navigate to a page(origin uri) depending on another page(source uri),
     * you will finally open the second page and you can get the origin uri through {@link Rabbit#KEY_ORIGIN_URI}
     */
    public static final String KEY_SOURCE_URI = "rabbits_source_uri";

    private static IRouter sRouter;
    static String sAppScheme;
    static String sDefaultHost;
    private static INavigatorFactory sNavigatorFactory;
//    private static List<INavigationInterceptor> sInterceptors;
//
//    private List<INavigationInterceptor> mInterceptors;
    private List<Interceptor> mInterceptors;
    private SparseArray<Navigator> mNavigators;

    private static class RabbitInvocationHandler implements InvocationHandler {

        private List<Class<?>> mClasses;
        private Map<String, Method> methods = new HashMap<>();

        @Override
        public Object invoke(Object o, Method method, Object[] objects) throws Throwable {
            if (mClasses == null) {
                mClasses = new ArrayList<>();
                Class cls;
                try {
                    cls = Class.forName(ROUTERS_CLASS);
                } catch (ClassNotFoundException e) {
                    cls = Class.forName(ROUTER_CLASS);
                    mClasses.add(cls);
                }
                if (mClasses.size() == 0) { // means using Routers class, so we fill the Array
                    Field field = cls.getField(ROUTERS_FIELD_CLASS);
                    String[] names = (String[]) field.get(null);
                    for (String name : names) {
                        try {
                            mClasses.add(Class.forName(PACKAGE + "." + name));
                        } catch (ClassNotFoundException e) {
                            Log.e(TAG, "Can not found class of name: " + PACKAGE + "." + name);
                        }
                    }
                }
            }
            String page = (String) objects[0];
            if (page == null || page.length() == 0) {
                return null;
            }
            String name = method.getName();

            String key = name + "-" + page;

            Method m = methods.get(key);
            if (m != null) {
                return m.invoke(null);
            } else {
                boolean findObtain = true;
                if (name.equals(IRouter.METHOD_OBTAIN)) {
                    String methodName = NameParser.parseObtain(page);
                    for (Class<?> clz : mClasses) {
                        try {
                            m = clz.getMethod(methodName);
                            break;
                        } catch (NoSuchMethodException e) {
                            // do nothing
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    if (m != null) {
                        methods.put(key, m);
                        return m.invoke(null);
                    }
                    findObtain = false;
                }
                if (name.equals(IRouter.METHOD_ROUTE) || !findObtain) {
                    String methodName = NameParser.parseRoute(page);
                    for (Class<?> clz : mClasses) {
                        try {
                            m = clz.getMethod(methodName);
                            break;
                        } catch (NoSuchMethodException e) {
                            // do nothing
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    if (m != null) {
                        methods.put(key, m);
                        return m.invoke(null);
                    }
                }
            }
            return null;
        }
    }

    private static  Rabbit sInstance;
    private Rabbit() { }

    private static Rabbit getInstance() {
//        synchronized (Rabbit.class) {
//            if (sInstance == null) {
//                synchronized (Rabbit.class) {
//                    sInstance = new Rabbit();
//                }
//            }
//        }
        return sInstance;
    }

//    private Rabbit(Object from) {
//        if (sRouter == null) {
//            sRouter = (IRouter) Proxy.newProxyInstance(IRouter.class.getClassLoader(), new Class[]{IRouter.class}, new RabbitInvocationHandler());
//        }
//    }

    /**
     * Dump mappings.
     *
     * @return result string.
     */
//    public static String dumpMappings() {
//        return Mappings.dump();
//    }

    public static void init(RConfig config) {
        if (!config.valid()) {
            throw new IllegalArgumentException("Config object not valid");
        }
        sInstance = new Rabbit();
        sInstance.registerNavigator(TargetInfo.TYPE_ACTIVITY, new ActivityNavigator());
        sAppScheme = config.getScheme();
        sDefaultHost = config.getHost();
        sNavigatorFactory = config.getNavigatorFactory() == null ? new DefaultNavigatorFactory() : config.getNavigatorFactory();
    }

    /**
     * Initial rabbits with basic elements.
     *
     * @param scheme           Scheme for this application.
     * @param defaultHost      Default host when try to match uri without a host.
     * @param navigatorFactory Custom navigator factory.
     */
//    private static void init(String scheme, String defaultHost, INavigatorFactory navigatorFactory) {
//        sAppScheme = scheme;
//        sDefaultHost = defaultHost;
//        sNavigatorFactory = navigatorFactory;
//    }

    /**
     * Create a rabbit who has ability to navigate through your pages.
     *
     * @param from Whether an Activity or a Fragment instance.
     * @return Rabbit instance.
     */
    public static Navigation from(Object from) {
        if (!(from instanceof Activity) && !(from instanceof Fragment || from instanceof android.app.Fragment) && !(from instanceof Context)) {
            throw new IllegalArgumentException("From object must be whether an Activity or a Fragment instance.");
        }
        Action action = new Action();
        action.setFrom(from);
        return new DefaultNavigation(getInstance(), action);
    }

//    public static Navigation redirect(Object from, Action action) {
//        action.setFrom(from);
//        return new DefaultNavigation(getInstance(), action);
//    }

    /**
     * Add global interceptor. These Interceptors' methods will be invoked in every navigation.
     *
     * @param interceptor Interceptor instance.
     */
//    public static void addGlobalInterceptor(INavigationInterceptor interceptor) {
//        if (sInterceptors == null) {
//            sInterceptors = new ArrayList<>();
//        }
//        sInterceptors.add(interceptor);
//    }

    /**
     * Add an interceptor used for this navigation. This is useful when you want to check whether a
     * uri matches a specific page using method.
     *
     * @param interceptor Interceptor instance.
     * @return Rabbit instance.
     */
    public Rabbit addInterceptor(Interceptor interceptor) {
        if (mInterceptors == null) {
            mInterceptors = new ArrayList<>();
        }
        mInterceptors.add(interceptor);
        return this;
    }

    public Rabbit registerNavigator(int type, Navigator navigator) {
        mNavigators.put(type, navigator);
        return this;
    }

    public Rabbit registerInterceptor(Interceptor interceptor, String pattern) {
        // TODO: 19/12/2017
        return this;
    }

    DispatchResult dispatch(Navigation navigation) {
        final Action action = navigation.action();

        // interceptors

        List<Interceptor> interceptors = new ArrayList<>(mInterceptors);
        interceptors.add(new NavigatorInterceptor(mNavigators));

        RealDispatcher dispatcher = new RealDispatcher(action, null, 0);
        return dispatcher.dispatch(action);
    }

    /**
     * Used for obtain page object. Intent or Fragment instance.
     *
     * @param uriStr uriStr
     * @return AbstractNavigator
     */
//    public AbstractNavigator obtain(String uriStr) {
//        Uri uri = Uri.parse(uriStr);
//        return obtain(uri);
//    }

    /**
     * Used for obtain page object. Intent or Fragment instance.
     *
     * @param uri uri
     * @return AbstractNavigator
     */
//    public AbstractNavigator obtain(Uri uri) {
//        Target target = Mappings.match(uri).obtain(sRouter);
//        return dispatch(target, false);
//    }

    /**
     * Navigate to page, or perform a not found strategy.
     *
     * @param uriStr uri string
     * @return AbstractNavigator
     */
//    public AbstractNavigator to(String uriStr) {
//        return to(uriStr, false);
//    }

    /**
     * Navigate to page, or perform a not found strategy.
     *
     * @param uriStr       uri string
     * @param ignoreParent whether ignore parent when navigate
     * @return AbstractNavigator
     */
//    public AbstractNavigator to(String uriStr, boolean ignoreParent) {
//        Uri uri = Uri.parse(uriStr);
//        return to(uri, ignoreParent);
//    }

    /**
     * Navigate to page, or perform a not found strategy.
     *
     * @param uri uri
     * @return AbstractNavigator
     */
//    public AbstractNavigator to(Uri uri) {
//        return to(uri, false);
//    }

    /**
     * Navigate to page, or perform a not found strategy.
     *
     * @param uri          uri
     * @param ignoreParent whether ignore parent when navigate
     * @return AbstractNavigator
     */
//    public AbstractNavigator to(Uri uri, boolean ignoreParent) {
//        Target target = Mappings.match(uri);
//        if (ignoreParent) {
//            target.obtain(sRouter);
//        } else {
//            target.route(sRouter);
//        }
////        return dispatch(target, false);
//        return null;
//    }

    /**
     * Navigate to page, or just return null if not found.
     *
     * @param uriStr uri
     * @return AbstractNavigator
     */
//    public AbstractNavigator tryTo(String uriStr) {
//        Uri uri = Uri.parse(uriStr);
//        return tryTo(uri);
//    }

    /**
     * Navigate to page, or just return null if not found.
     *
     * @param uriStr       uri
     * @param ignoreParent whether ignore parent when navigate
     * @return AbstractNavigator
     */
//    public AbstractNavigator tryTo(String uriStr, boolean ignoreParent) {
//        Uri uri = Uri.parse(uriStr);
//        return tryTo(uri, ignoreParent);
//    }

    /**
     * Only if it is a http/https uri, and a page mapping from uri with app scheme and given path
     * exists, a valid navigator will returned.
     *
     * @param uri uri
     * @return AbstractNavigator
     */
//    public AbstractNavigator tryTo(Uri uri) {
//        return tryTo(uri, false);
//    }

    /**
     * Only if it is a http/https uri, and a page mapping from uri with app scheme and given path
     * exists, a valid navigator will returned.
     *
     * @param uri          uri
     * @param ignoreParent whether ignore parent when navigate
     * @return AbstractNavigator
     */
//    public AbstractNavigator tryTo(Uri uri, boolean ignoreParent) {
//        final String scheme = uri.getScheme();
//        if (TextUtils.isEmpty(scheme) || (!scheme.startsWith("http") && !scheme.startsWith(sAppScheme))) {
//            return new MuteNavigator(mFrom, new Target(uri), assembleInterceptor());
//        }
//        if (!TextUtils.equals(scheme, sAppScheme)) {
//            uri = uri.buildUpon().scheme(sAppScheme).build();
//        }
//        Target target = Mappings.match(uri);
//        if (ignoreParent) {
//            target.obtain(sRouter);
//        } else {
//            target.route(sRouter);
//        }
//        return dispatch(target, true);
//    }

    /**
     * Handle the intercept operation.
     *
     * @param target target
     * @param mute   whether mute
     * @return navigator
     */
//    private AbstractNavigator dispatch(Target target, boolean mute) {
//        if (BuildConfig.DEBUG) {
//            Log.d(TAG, target.toString());
//        }
//        if (!target.hasMatched()) {
//            if (!mute) {
//                AbstractPageNotFoundHandler pageNotFoundHandler = sNavigatorFactory.createPageNotFoundHandler(mFrom, target, assembleInterceptor());
//                if (pageNotFoundHandler != null) {
//                    return pageNotFoundHandler;
//                }
//            } else if (target.getTo() == null) {
//                return new MuteNavigator(mFrom, target, assembleInterceptor());
//            }
//        }
//        return sNavigatorFactory.createNavigator(mFrom, target, assembleInterceptor());
//    }

    /**
     * Assemble interceptors and static interceptors.
     * order static interceptors after instance's interceptors.
     *
     * @return a list of valid navigation interceptor
     */
//    private List<INavigationInterceptor> assembleInterceptor() {
//        if (sInterceptors == null) {
//            return mInterceptors;
//        }
//        if (mInterceptors == null) {
//            mInterceptors = new ArrayList<>();
//        }
//        mInterceptors.addAll(sInterceptors);
//        return mInterceptors;
//    }
}
