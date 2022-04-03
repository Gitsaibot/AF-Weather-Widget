package net.gitsaibot.af;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.View;

import androidx.appcompat.widget.AppCompatImageView;
import androidx.preference.DialogPreference;
import androidx.preference.PreferenceViewHolder;

public class ColorPreference extends DialogPreference implements android.view.View.OnClickListener {

	private int mValue;
	private int mDefaultValue;
	
	private ColorView mColorView;
	private View mRevertView;

	public static class RevertHolder extends AppCompatImageView {
        public RevertHolder(Context context, AttributeSet attrs) {
            super(context, attrs);
        }

        @Override
        public void setPressed(boolean pressed) {
            // If the parent is pressed, do not set to pressed.
            if (pressed && ((View) getParent()).isPressed()) {
                return;
            }
            super.setPressed(pressed);
        }
    }

	public ColorPreference(Context context) {
		this(context, null);
	}

	public ColorPreference(Context context, AttributeSet attrs) {
		this(context, attrs, R.attr.colorPreferenceStyle);
	}
	
	public ColorPreference(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		setDialogLayoutResource(R.layout.dialog_color);
	}

	@Override
	public void onBindViewHolder(PreferenceViewHolder view) {
		super.onBindViewHolder(view);
		mColorView = (ColorView)view.findViewById(R.id.color);
		mColorView.setColor(getValue());

		mRevertView = view.findViewById(R.id.revert);
		mRevertView.setOnClickListener(this);

		// Set listview item padding to 0 so revert button matches right edge
		((View)mRevertView.getParent().getParent().getParent()).setPadding(0, 0, 0, 0);
	}

	/* mRevertView onClick() */
	@Override
	public void onClick(View v) {
		v.post(() -> setValue(mDefaultValue));
	}

	public int getValue() {
		return mValue;
	}

	@Override
	protected Object onGetDefaultValue(TypedArray a, int index) {
		mDefaultValue = a.getInteger(index, 0);
		return mDefaultValue;
	}

	@Override
	protected void onSetInitialValue(boolean restorePersistedValue,
			Object defaultValue)
	{
		setValue(restorePersistedValue ? getPersistedInt(mValue) : (Integer) defaultValue);
	}

	public void setValue(int value) {
		mValue = value;
		if (mColorView != null) {
			mColorView.setColor(value);
		}
		persistInt(value);
		notifyChanged();
	}

}
