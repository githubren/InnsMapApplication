package com.example.yfsl.innsmapapplication;

public interface ICallBack<T> {
    void onSuccess(T object);

    void onFail();
}
