package com.skin.rbx.clothes.makek.ui.outfit.c253.colorpicker;

public interface ColorObservable {

    void subscribe(ColorObserver observer);

    void unsubscribe(ColorObserver observer);

    int getColor();
}
