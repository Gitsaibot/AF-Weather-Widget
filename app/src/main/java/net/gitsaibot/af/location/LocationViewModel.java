package net.gitsaibot.af.location;

import android.app.Application;
import android.content.ContentUris;
import android.content.ContentValues;
import android.database.Cursor;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import net.gitsaibot.af.AfProvider.AfLocations;
import net.gitsaibot.af.AfUtils;
import net.gitsaibot.af.BuildConfig;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class LocationViewModel extends AndroidViewModel {

    private final MutableLiveData<Cursor> locationsCursor = new MutableLiveData<>();
    private final MutableLiveData<List<AfAddress>> searchResults = new MutableLiveData<>();
    private final MutableLiveData<Integer> searchStatus = new MutableLiveData<>();

    private static final List<String> geonamesDetailedNameComponents = List.of("name", "adminName5", "adminName4", "adminName3", "adminName2", "adminName1", "countryName");

    public final static int INVALID_INPUT = 0;
    public final static int NO_CONNECTION = 1;
    public final static int SEARCH_CANCELLED = 2;
    public final static int SEARCH_SUCCESS = 4;
    public final static int NO_RESULTS = 5;
    public final static int OVER_QUERY_LIMIT = 6;
    public final static int REQUEST_DENIED = 7;
    public final static int INVALID_REQUEST = 8;
    public final static int SEARCH_ERROR = 100;

    public LocationViewModel(@NonNull Application application) {
        super(application);
        loadLocations();
    }

    public LiveData<Cursor> getLocations() {
        return locationsCursor;
    }

    public LiveData<List<AfAddress>> getSearchResults() {
        return searchResults;
    }

    public LiveData<Integer> getSearchStatus() {
        return searchStatus;
    }

    public void loadLocations() {
        new Thread(() -> {
            Cursor cursor = getApplication().getContentResolver().query(
                    AfLocations.CONTENT_URI,
                    null, null, null, null
            );
            locationsCursor.postValue(cursor);
        }).start();
    }

    public void deleteLocation(long id) {
        new Thread(() -> {
            getApplication().getContentResolver().delete(
                    ContentUris.withAppendedId(AfLocations.CONTENT_URI, id),
                    null, null);
            loadLocations();
        }).start();
    }

    public void updateLocationTitle(long id, String displayTitle) {
        new Thread(() -> {
            ContentValues values = new ContentValues();
            values.put(AfLocations.TITLE, displayTitle);
            getApplication().getContentResolver().update(
                    ContentUris.withAppendedId(AfLocations.CONTENT_URI, id),
                    values, null, null);
            loadLocations();
        }).start();
    }

    public void searchLocations(String query) {
        searchStatus.postValue(-1); // Busy
        new Thread(() -> {
            if (query == null || query.trim().isEmpty()) {
                searchStatus.postValue(INVALID_INPUT);
                return;
            }

            try {
                final int MAX_RESULTS = 7;
                URI uri = new URI("https", "secure.geonames.org", "/searchJSON",
                        "q=" + query.trim() +
                                "&lang=" + Locale.getDefault().getLanguage() +
                                "&maxRows=" + MAX_RESULTS +
                                "&username=" + BuildConfig.USER_GEONAMES, null);

                HttpURLConnection httpClient = AfUtils.setupHttpClient(uri.toURL(), getApplication());
                InputStream content = AfUtils.getGzipInputStream(httpClient);
                JSONObject jObject = new JSONObject(AfUtils.convertStreamToString(content));

                if (jObject.has("status")) {
                    int errorCode = jObject.getJSONObject("status").optInt("value", 0);
                    searchStatus.postValue(SEARCH_ERROR + errorCode);
                    return;
                }

                List<AfAddress> addresses = new ArrayList<>();
                JSONArray results = jObject.getJSONArray("geonames");
                int numResults = Math.min(results.length(), MAX_RESULTS);

                if (numResults <= 0) {
                    searchStatus.postValue(NO_RESULTS);
                    return;
                }

                for (int i = 0; i < numResults; i++) {
                    try {
                        JSONObject result = results.getJSONObject(i);
                        AfAddress address = new AfAddress();
                        address.title = result.getString("name");
                        address.title_detailed = buildTitleDetailed(result);
                        address.latitude = result.getString("lat");
                        address.longitude = result.getString("lng");
                        addresses.add(address);
                    } catch (Exception e) { /* ignore malformed results */ }
                }

                if (!addresses.isEmpty()) {
                    searchResults.postValue(addresses);
                    searchStatus.postValue(SEARCH_SUCCESS);
                } else {
                    searchStatus.postValue(NO_RESULTS);
                }
            } catch (UnknownHostException e) {
                searchStatus.postValue(NO_CONNECTION);
            } catch (Exception e) {
                e.printStackTrace();
                searchStatus.postValue(SEARCH_ERROR);
            }
        }).start();
    }

    private static String buildTitleDetailed(final JSONObject result) {
        StringBuilder titleDetailedSb = new StringBuilder();

        for (String key : geonamesDetailedNameComponents) {
            if (result.has(key)) {
                String component = result.optString(key).trim();

                if (!component.isEmpty()) {
                    if (titleDetailedSb.length() > 0) {
                        titleDetailedSb.append(", ");
                    }
                    titleDetailedSb.append(component);
                }
            }
        }
        return titleDetailedSb.toString();
    }
}
