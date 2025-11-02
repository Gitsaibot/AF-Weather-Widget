package net.gitsaibot.af.location;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.SimpleCursorAdapter;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import net.gitsaibot.af.AfProvider.AfLocations;
import net.gitsaibot.af.AfProvider.AfLocationsColumns;
import net.gitsaibot.af.R;

import java.util.List;
import java.util.Map;

public class AfLocationSelectionActivity extends AppCompatActivity implements View.OnClickListener {

    private LocationViewModel locationViewModel;
    private SimpleCursorAdapter mAdapter;
    private ProgressBar mProgressBar;
    private static final Map<Integer, String> geonamesWebserviceExceptions;

    static {
        geonamesWebserviceExceptions = Map.ofEntries(
                Map.entry(10, "Authorization exception"),
                Map.entry(11, "Record does not exist"),
                Map.entry(12, "Other error"),
                Map.entry(13, "Database timeout"),
                Map.entry(14, "Invalid parameter"),
                Map.entry(15, "No result found"),
                Map.entry(16, "Duplicate exception"),
                Map.entry(17, "Postal code not found"),
                Map.entry(18, "Daily limit of credits exceeded"),
                Map.entry(19, "Hourly limit of credits exceeded"),
                Map.entry(20, "Weekly limit of credits exceeded"),
                Map.entry(21, "Invalid input"),
                Map.entry(22, "Server overloaded exception"),
                Map.entry(23, "Service not implemented"),
                Map.entry(24, "Radius too large")
        );
    }

    private static final String TAG = "AfLocationSelection";

    private static final int CONTEXT_MENU_EDIT = Menu.FIRST;
    private static final int CONTEXT_MENU_DELETE = Menu.FIRST + 1;

    private Context mContext = null;
    private Cursor mCursor = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = this;
        setContentView(R.layout.location_selection_list);

        ListView listView = findViewById(android.R.id.list);
        listView.setEmptyView(findViewById(R.id.ListItemTitle));
        mProgressBar = findViewById(R.id.progress_bar);

        Button addLocationButton = findViewById(R.id.add_location_button);
        addLocationButton.setOnClickListener(this);

        mAdapter = new SimpleCursorAdapter(
                mContext,
                R.layout.location_selection_row,
                null,
                new String[]{
                        AfLocationsColumns.TITLE_DETAILED,
                        AfLocationsColumns.TITLE,
                        AfLocationsColumns.LATITUDE,
                        AfLocationsColumns.LONGITUDE
                },
                new int[]{
                        R.id.location_selection_row_title,
                        R.id.location_selection_row_display_title,
                        R.id.location_selection_row_latitude,
                        R.id.location_selection_row_longitude
                }, 0);
        listView.setAdapter(mAdapter);
        listView.setOnItemClickListener((parent, view, position, id) -> {
            Intent intent = new Intent();
            String result = ContentUris.withAppendedId(AfLocations.CONTENT_URI, id).toString();
            intent.putExtra("location", result);
            setResult(Activity.RESULT_OK, intent);
            finish();
        });
        registerForContextMenu(listView);

        locationViewModel = new ViewModelProvider(this).get(LocationViewModel.class);
        locationViewModel.getLocations().observe(this, cursor -> {
            mAdapter.swapCursor(cursor);
            mCursor = cursor;
        });

        locationViewModel.getSearchResults().observe(this, this::handleSearchResults);
        locationViewModel.getSearchStatus().observe(this, this::handleSearchStatus);
    }

    private void handleSearchResults(List<AfAddress> addresses) {
        String[] listItems = new String[addresses.size()];
        for (int i = 0; i < addresses.size(); i++) {
            listItems[i] = addresses.get(i).title_detailed;
        }
        AlertDialog alertDialog = new MaterialAlertDialogBuilder(mContext)
                .setTitle(R.string.location_search_results_select_dialog_title)
                .setItems(listItems, (dialog, which) -> {
                    AfAddress a = addresses.get(which);
                    ContentResolver resolver = mContext.getContentResolver();
                    ContentValues values = new ContentValues();
                    values.put(AfLocationsColumns.LATITUDE, a.latitude);
                    values.put(AfLocationsColumns.LONGITUDE, a.longitude);
                    values.put(AfLocationsColumns.TITLE, a.title);
                    values.put(AfLocationsColumns.TITLE_DETAILED, a.title_detailed);
                    resolver.insert(AfLocations.CONTENT_URI, values);
                    locationViewModel.loadLocations();
                })
                .create();
        alertDialog.show();
    }

    private void handleSearchStatus(Integer status) {
        if (status == null) return;
        if (status == -1) { // Busy
            mProgressBar.setVisibility(View.VISIBLE);
            return;
        }

        mProgressBar.setVisibility(View.GONE);

        if (status >= LocationViewModel.SEARCH_ERROR) {
            final int errorCode = status - LocationViewModel.SEARCH_ERROR;
            final String errorString = geonamesWebserviceExceptions.getOrDefault(errorCode, getString(R.string.location_search_error_toast));
            Toast.makeText(mContext, errorString, Toast.LENGTH_SHORT).show();
            return;
        }

        switch (status) {
            case LocationViewModel.INVALID_INPUT -> Toast.makeText(
                    mContext,
                    getString(R.string.invalid_search_input_toast),
                    Toast.LENGTH_SHORT).show();
            case LocationViewModel.NO_CONNECTION -> Toast.makeText(
                    mContext,
                    getString(R.string.location_search_no_connection_toast),
                    Toast.LENGTH_SHORT).show();
            case LocationViewModel.SEARCH_CANCELLED -> Log.d(TAG, "Search was cancelled!");
            case LocationViewModel.NO_RESULTS -> Toast.makeText(
                    AfLocationSelectionActivity.this,
                    getString(R.string.location_search_no_results),
                    Toast.LENGTH_SHORT).show();
            case LocationViewModel.OVER_QUERY_LIMIT -> Toast.makeText(
                    AfLocationSelectionActivity.this,
                    getString(R.string.location_search_over_query_limit_toast),
                    Toast.LENGTH_LONG).show();
            case LocationViewModel.REQUEST_DENIED -> Toast.makeText(
                    AfLocationSelectionActivity.this,
                    getString(R.string.location_search_request_denied_toast),
                    Toast.LENGTH_SHORT).show();
            case LocationViewModel.INVALID_REQUEST -> Toast.makeText(
                    AfLocationSelectionActivity.this,
                    getString(R.string.location_search_invalid_request_toast),
                    Toast.LENGTH_SHORT).show();
        }
    }


    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        AdapterView.AdapterContextMenuInfo adapterMenuInfo = (AdapterView.AdapterContextMenuInfo) (menuInfo);
        if (adapterMenuInfo == null) {
            return;
        }
        mCursor.moveToPosition(adapterMenuInfo.position);
        if (mCursor.isAfterLast()) return;
        menu.setHeaderTitle(String.format(getString(R.string.location_list_context_title), mCursor.getString(mCursor.getColumnIndexOrThrow(AfLocationsColumns.TITLE))));
        menu.add(0, CONTEXT_MENU_EDIT, 0, getString(R.string.location_list_context_edit));
        menu.add(0, CONTEXT_MENU_DELETE, 0, getString(R.string.location_list_context_delete));
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo adapterMenuInfo = (AdapterView.AdapterContextMenuInfo) (item.getMenuInfo());
        if (adapterMenuInfo == null) {
            return super.onContextItemSelected(item);
        }
        mCursor.moveToPosition(adapterMenuInfo.position);
        if (mCursor.isAfterLast()) return false;

        long locationId = mCursor.getLong(mCursor.getColumnIndexOrThrow(BaseColumns._ID));

        switch (item.getItemId()) {
            case CONTEXT_MENU_DELETE:
                locationViewModel.deleteLocation(locationId);
                return true;
            case CONTEXT_MENU_EDIT:
                String locationName = mCursor.getString(mCursor.getColumnIndexOrThrow(AfLocations.TITLE));
                DialogFragment editFragment = EditLocationDialogFragment.newInstance(locationId, locationName);
                editFragment.show(getSupportFragmentManager(), "edit_location");
                return true;
        }
        return false;
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.add_location_button) {
            DialogFragment addFragment = new AddLocationDialogFragment();
            addFragment.show(getSupportFragmentManager(), "add_location");
        }
    }
}
