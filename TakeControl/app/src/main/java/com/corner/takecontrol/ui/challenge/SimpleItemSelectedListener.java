package com.corner.takecontrol.ui.challenge;

import android.widget.AdapterView;

abstract class SimpleItemSelectedListener implements AdapterView.OnItemSelectedListener {
    @Override
    public void onItemSelected(AdapterView<?> parent, android.view.View view, int position, long id) {
        onItemSelected(position);
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
    }

    abstract void onItemSelected(int position);
}
