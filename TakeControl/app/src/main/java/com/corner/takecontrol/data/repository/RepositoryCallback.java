package com.corner.takecontrol.data.repository;

public interface RepositoryCallback<T> {
    void onSuccess(T result);

    default void onError(String message) {}
}
