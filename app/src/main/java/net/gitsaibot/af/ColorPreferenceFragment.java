package net.gitsaibot.af;

import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.preference.DialogPreference;
import androidx.preference.PreferenceDialogFragmentCompat;

import java.util.Locale;

public class ColorPreferenceFragment extends PreferenceDialogFragmentCompat implements ColorView.OnValueChangeListener {

    private int mValue;

    private boolean showHexDialog = false;

    private View mColorPickerContainer;
    private View mHexInputContainer;

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
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean("showHexDialog", showHexDialog);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            showHexDialog = savedInstanceState.getBoolean("showHexDialog", false);
        }
    }

    @Override
    public void onDialogClosed(boolean positiveResult) {
        if (positiveResult) {
            DialogPreference preference = getPreference();
            if (preference instanceof ColorPreference colorPreference) {
                colorPreference.setValue(mValue);
            }
        }
    }

    @Override
    protected void onPrepareDialogBuilder(AlertDialog.Builder builder) {
        super.onPrepareDialogBuilder(builder);
        builder.setTitle(showHexDialog ? "Input Hex Color" : "Select Color");
        builder.setPositiveButton(android.R.string.ok, this);
        builder.setNegativeButton(android.R.string.cancel, this);
        builder.setNeutralButton(showHexDialog ? "Color Picker" : "Hex Input", null);
    }

    @Override
    public void onStart() {
        super.onStart();
        AlertDialog dialog = (AlertDialog) getDialog();
        if (dialog != null) {
            Button neutralButton = dialog.getButton(DialogInterface.BUTTON_NEUTRAL);
            neutralButton.setOnClickListener(v -> {
                showHexDialog = !showHexDialog;
                updateViewVisibility();
                dialog.setTitle(showHexDialog ? "Input Hex Color" : "Select Color");
                neutralButton.setText(showHexDialog ? "Color Picker" : "Hex Input");
            });
        }
    }

    @Override
    protected void onBindDialogView(View view) {
        super.onBindDialogView(view);

        mColorPickerContainer = view.findViewById(R.id.colorPickerContainer);
        mHexInputContainer = view.findViewById(R.id.hexInputContainer);

        mAlphaSlider = view.findViewById(R.id.alphaSlider);
        mHueSlider = view.findViewById(R.id.hueSlider);
        mSvMap = view.findViewById(R.id.svMap);
        mAlphaTextView = view.findViewById(R.id.alphaText);
        mHueTextView = view.findViewById(R.id.hueText);
        mSaturationTextView = view.findViewById(R.id.saturationText);
        mValueTextView = view.findViewById(R.id.valueText);
        mColorOld = view.findViewById(R.id.colorOld);
        mColorNew = view.findViewById(R.id.colorNew);
        mEditText = view.findViewById(R.id.edittext);

        mAlphaSlider.setOnValueChangeListener(this);
        mHueSlider.setOnValueChangeListener(this);
        mSvMap.setOnValueChangeListener(this);

        setupDialogValues();
        updateLabels();
        updateViewVisibility();
    }

    private void updateViewVisibility() {
        mHexInputContainer.setVisibility(showHexDialog ? View.VISIBLE : View.GONE);
        mColorPickerContainer.setVisibility(showHexDialog ? View.GONE : View.VISIBLE);
        if (showHexDialog) {
            int color = Color.HSVToColor(Math.round(mAlpha * 255.0f), mHSV);
            mEditText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
            mEditText.setText(String.format(Locale.getDefault(), "#%08X", color));
        }
    }

    private void setupDialogValues() {
        DialogPreference preference = getPreference();
        if (preference instanceof ColorPreference) {
            mValue = ((ColorPreference) preference).getValue();
        }

        Color.colorToHSV(mValue, mHSV);
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
        mAlphaTextView.setText(String.format(Locale.getDefault(),"A: %.0f%%", mAlpha * 100.0f));
        mHueTextView.setText(String.format(Locale.getDefault(),"H: %.0f°", mHSV[0]));
        mSaturationTextView.setText(String.format(Locale.getDefault(),"S: %.0f%%", mHSV[1] * 100.0f));
        mValueTextView.setText(String.format(Locale.getDefault(),"V: %.0f%%", mHSV[2] * 100.0f));
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        if (which == DialogInterface.BUTTON_POSITIVE) {
            if (showHexDialog) {
                try {
                    mValue = Color.parseColor(mEditText.getText().toString());
                } catch (Exception e) {
                    // This is not ideal, as the dialog will close. A better implementation
                    // would also override the positive button's click listener.
                    Toast.makeText(getContext(), "Invalid color code entered", Toast.LENGTH_SHORT).show();
                    return; // Don't close dialog by not calling super
                }
            } else {
                mValue = Color.HSVToColor(Math.round(mAlpha * 255.0f), mHSV);
            }
        }
        super.onClick(dialog, which);
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
