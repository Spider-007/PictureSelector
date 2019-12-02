

package com.luck.picture.lib.rxbus2;

import android.support.annotation.NonNull;

import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.observers.DisposableObserver;
import io.reactivex.schedulers.Schedulers;

public class RxUtils {

    /**基于rx开发*/
    public static <T> DisposableObserver computation(final RxUtils.RxSimpleTask task, Object... objects) {
        return computation(0, task, objects);
    }

    /***/
    public static <T> DisposableObserver computation(long delayMilliseconds, final RxUtils.RxSimpleTask task, Object... objects) {
        Observable observable = Observable.create((e) -> {
            Object obj = task.doSth(objects);
            if (obj == null) {
                obj = new Object();
            }
            e.onNext(obj);
            e.onComplete();
        }).delay(delayMilliseconds, TimeUnit.MILLISECONDS)
                .subscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread());
        DisposableObserver disposableObserver = new DisposableObserver<T>() {
            @Override
            public void onNext(T o) {
                if (!this.isDisposed()) {
                    task.onNext(o);
                }
            }

            @Override
            public void onError(Throwable e) {
                if (!this.isDisposed()) {
                    task.onError(e);
                }
            }

            @Override
            public void onComplete() {
                if (!this.isDisposed()) {
                    task.onComplete();
                }
            }
        };
        observable.subscribe(disposableObserver);
        return disposableObserver;
    }


    public static <T> void newThread(final RxUtils.RxSimpleTask task, Object... objects) {
        newThread(0, task, objects);
    }

    public static <T> void newThread(long delayMilliseconds, final RxUtils.RxSimpleTask task, Object... objects) {
        Observable observable = Observable.create((e) -> {
//            LogUtils.i("newThread subscribe");
            Object obj = task.doSth(objects);
            if (obj == null) {
                obj = new Object();
            }
            e.onNext(obj);
            e.onComplete();
        }).delay(delayMilliseconds, TimeUnit.MILLISECONDS)
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread());
        observable.subscribe(new DisposableObserver<T>() {
            @Override
            public void onNext(T o) {
                if (!this.isDisposed()) {
                    task.onNext(o);
                }
            }

            @Override
            public void onError(Throwable e) {
                if (!this.isDisposed()) {
                    task.onError(e);
                }
            }

            @Override
            public void onComplete() {
                if (!this.isDisposed()) {
                    task.onComplete();
                }
            }
        });
    }

    /**Rx的io获取数据显示*/
    public static <T> void io(final RxUtils.RxSimpleTask task) {
        io(0, task);
    }


    /**
     * @params delayMilliseconds
     * @paremas task 任务，传入任意类型的集合
     */
    public static <T> void io(long delayMilliseconds, final RxUtils.RxSimpleTask task) {

        /**rxjava 观察者模式回调，显示创建实例*/
        Observable observable = Observable.create((e) -> {
            /**获取默认值*/
            Object obj = task.doSth(new Object[0]);
            /**判断为空*/
            if (obj == null) {
                obj = new <T>Object();
            }

            /**获取下一个节点的 对象*/
            e.onNext(obj);
            /**完成*/
            e.onComplete();
            /**
             * 返回用于计算工作的默认共享Scheduler实例。
             * 这可以用于事件循环，处理回调和其他计算工作。
             * 不建议在此调度程序上执行阻塞的，与IO绑定的工作。使用io（）代替*/
        }).delay(delayMilliseconds, TimeUnit.MILLISECONDS)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
        /**得到观察者模式的对象*/
        observable.subscribe(new DisposableObserver<T>() {
            @Override
            public void onNext(T o) {
                if (!this.isDisposed()) {
                    task.onNext(o);
                }
            }

            @Override
            public void onError(Throwable e) {
                if (!this.isDisposed()) {
                    task.onError(e);
                }
            }

            @Override
            public void onComplete() {
                if (!this.isDisposed()) {
                    task.onComplete();
                }
            }
        });
    }

    private RxUtils() {
    }

    /**
     * Rx处理任务的回调
     */
    public abstract static class RxSimpleTask<T> {

        /**
         * 获取默认值，默认为空
         */
        public T getDefault() {
            return null;
        }

        /**
         * 也是获取默认值，直接调用的getDefault
         */
        public @NonNull
        T doSth(Object... objects) {
            return getDefault();
        }

        /**
         * 返回数据
         */
        public void onNext(T returnData) {
        }

        /**
         * 异常数据
         */
        public void onError(Throwable e) {
        }

        /**
         * 完成
         */
        public void onComplete() {
        }
    }
}
