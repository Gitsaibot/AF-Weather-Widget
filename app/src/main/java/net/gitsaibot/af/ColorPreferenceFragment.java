package net.gitsaibot.af;

import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.preference.DialogPreference;
import androidx.preference.PreferenceDialogFragmentCompat;

public class ColorPreferenceFragment extends PreferenceDialogFragmentCompat implements ColorView.OnValueChangeListener {

    private int mValue;
    private int mDefaultValue;

    private ColorView mColorView;
    private View mRevertView;

    private boolean showHexDialog = false;

    /* Dialog stuff */
    private ColorView mAlphaSlider;
    private ColorView mColorNew;
    private ColorView mColorOld;
    private ColorView mHueSlider;
    private ColorView mSvMap;

    private TextView mAlphaTextView;
    private TextView mHueTextView;
    private TextView mSaturationTextView;
    private TextView mValueTextView;

    private float[] mHSV = new float[3];
    private float mAlpha;

    private EditText mEditText;

    public static ColorPreferenceFragment newInstance(
            String key) {
        final ColorPreferenceFragment
                fragment = new ColorPreferenceFragment();
        final Bundle b = new Bundle(1);
        b.putString(ARG_KEY, key);
        fragment.setArguments(b);

        return fragment;
    }

    @Override
    public void onDialogClosed(boolean positiveResult) {
        if (positiveResult) {

            // Get the related Preference and save the value
            DialogPreference preference = getPreference();
            if (preference instanceof ColorPreference) {
                ColorPreference colorPreference =
                        ((ColorPreference) preference);
                // This allows the client to ignore the user value.
                // Save the value
                colorPreference.setValue(Color.HSVToColor(Math.round(mAlpha * 255.0f), mHSV));

            }
        }
    }

    @Override
    protected void onPrepareDialogBuilder(AlertDialog.Builder builder) {
        super.onPrepareDialogBuilder(builder);

        if (showHexDialog) {
            builder.setTitle("Input Hex Color");
        }

        builder.setPositiveButton(android.R.string.ok, this);
        builder.setNegativeButton(android.R.string.cancel, this);

        //ToDo Get this running again!
        //if (!showHexDialog) {
        //    builder.setNeutralButton("Hex Input", this);
        //}
    }

    @Override
    protected void onBindDialogView(View view) {

        if (showHexDialog) {
            int color = Color.HSVToColor(Math.round(mAlpha * 255.0f), mHSV);

            mEditText = view.findViewById(R.id.edittext);
            mEditText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
            mEditText.setText(String.format("#%08X", color));
        } else {
            mAlphaSlider = view.findViewById(R.id.alphaSlider);
            mAlphaSlider.setOnValueChangeListener(this);
            mHueSlider = view.findViewById(R.id.hueSlider);
            mHueSlider.setOnValueChangeListener(this);

            mSvMap = view.findViewById(R.id.svMap);
            mSvMap.setOnValueChangeListener(this);

            mAlphaTextView = view.findViewById(R.id.alphaText);
            mHueTextView = view.findViewById(R.id.hueText);
            mSaturationTextView = view.findViewById(R.id.saturationText);
            mValueTextView = view.findViewById(R.id.valueText);

            mColorOld = view.findViewById(R.id.colorOld);
            mColorNew = view.findViewById(R.id.colorNew);

            setupDialogValues();
            updateLabels();
        }
    }

    private void setupDialogValues() {
        mHSV = new float[3];
        // Get the color from the related Preference
        Integer mValue = null;
        DialogPreference preference = getPreference();
        if (preference instanceof ColorPreference) {
            mValue = ((ColorPreference) preference).getValue();
        }
        Color.RGBToHSV(
                Color.red(mValue),
                Color.green(mValue),
                Color.blue(mValue),
                mHSV);

        mAlpha = (float)Color.alpha(mValue) / 255.0f;

        mHueSlider.setValue(new float[] { mHSV[0] / 360.0f });
        mAlphaSlider.setValue(new float[] { mAlpha });
        mAlphaSlider.setColor(mValue);
        mSvMap.setValue(new float[] { mHSV[1], mHSV[2] });
        mSvMap.setHue(mHSV[0]);
        mColorOld.setColor(mValue);
        mColorNew.setColor(mValue);
    }

    private void updateLabels() {
        mAlphaTextView.setText(String.format("A: %.0f%%", mAlpha * 100.0f));
        mHueTextView.setText(String.format("H: %.0f\u00b0", mHSV[0]));
        mSaturationTextView.setText(String.format("S: %.0f%%", mHSV[1] * 100.0f));
        mValueTextView.setText(String.format("V: %.0f%%", mHSV[2] * 100.0f));
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        super.onClick(dialog, which);

        switch (which) {
            case DialogInterface.BUTTON_POSITIVE:
                if (showHexDialog) {
                    try {
                        setValue(Color.parseColor(mEditText.getText().toString()));
                    } catch (Exception e) {
                        Toast.makeText(getContext(), "Invalid color code entered", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    setValue(Color.HSVToColor(Math.round(mAlpha * 255.0f), mHSV));
                }
                break;
            case DialogInterface.BUTTON_NEUTRAL:
                View view = View.inflate(getActivity(), R.layout.preference_widget_color,null);
                mRevertView = view.findViewById(R.id.revert);
                mRevertView.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        showHexDialog = true;

                    }
                }, 100);
                break;
            case DialogInterface.BUTTON_NEGATIVE:
                // TODO: End the world?
                break;
        }
    }

    private void setValue(int value) {
        mValue = value;
        if (mColorView != null) {
            mColorView.setColor(value);
        }
    }

    @Override
    public void updateValue(float[] value, Object source) {
        if (source == mAlphaSlider) {
            mAlpha = value[0];
        } else if (source == mHueSlider) {
            mHSV[0] = value[0] * 360.0f;
            mSvMap.setHue(mHSV[0]);
        } else if (source == mSvMap) {
            mHSV[1] = value[0];
            mHSV[2] = value[1];
        }

        int rgb = Color.HSVToColor(Math.round(mAlpha * 255.0f), mHSV);

        if (source != mAlphaSlider) {
            mAlphaSlider.setColor(rgb);
        }

        mColorNew.setColor(rgb);

        updateLabels();
    }
}
