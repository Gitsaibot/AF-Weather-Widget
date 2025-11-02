package net.gitsaibot.af.location;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import net.gitsaibot.af.R;

public class EditLocationDialogFragment extends DialogFragment {

    private LocationViewModel locationViewModel;
    private long locationId;
    private String locationName;

    public static EditLocationDialogFragment newInstance(long locationId, String locationName) {
        EditLocationDialogFragment fragment = new EditLocationDialogFragment();
        Bundle args = new Bundle();
        args.putLong("location_id", locationId);
        args.putString("location_name", locationName);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            locationId = getArguments().getLong("location_id");
            locationName = getArguments().getString("location_name");
        }
        locationViewModel = new ViewModelProvider(requireActivity()).get(LocationViewModel.class);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        View content = requireActivity().getLayoutInflater().inflate(R.layout.dialog_edittext, null);
        EditText editText = content.findViewById(R.id.edittext);
        editText.setText(locationName);
        editText.setSelection(locationName != null ? locationName.length() : 0);

        return new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Display title:")
                .setView(content)
                .setPositiveButton(android.R.string.ok, (pDialog, which) -> {
                    InputMethodManager imm = (InputMethodManager) requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(editText.getWindowToken(), 0);
                    String displayTitle = editText.getText().toString();

                    if (TextUtils.isEmpty(displayTitle)) {
                        Toast.makeText(requireContext(), "Invalid display title", Toast.LENGTH_SHORT).show();
                    } else {
                        locationViewModel.updateLocationTitle(locationId, displayTitle);
                    }
                })
                .setNegativeButton(android.R.string.cancel, (nDialog, which) -> {
                    InputMethodManager imm = (InputMethodManager) requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(editText.getWindowToken(), 0);
                    nDialog.cancel();
                })
                .create();
    }
}
