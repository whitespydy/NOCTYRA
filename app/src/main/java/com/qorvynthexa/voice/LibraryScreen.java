package com.qorvynthexa.voice;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.ViewGroup;
import android.view.View;
import android.graphics.Canvas;
import android.graphics.Paint;

import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Locale;
import java.time.Instant;

public final class LibraryScreen {
    private LibraryScreen() {}

    // Each entitlement is keyed to one individual catalogue book.


    public static void show(Activity activity) {
        try {
            showCollections(activity, LibraryCatalog.load(activity));
        } catch (Exception e) {
            Toast.makeText(activity, "Library catalogue could not be loaded.",
                    Toast.LENGTH_LONG).show();
        }
    }

    private static void showCollections(Activity a, JSONObject catalogue)
            throws Exception {
        JSONArray collections = catalogue.getJSONArray("collections");
        ArrayList<String> all = new ArrayList<>();

        for (int i = 0; i < collections.length(); i++) {
            JSONObject c = collections.getJSONObject(i);
            all.add(c.getString("name"));
        }

        EditText search = new EditText(a);
        search.setSingleLine(true);
        search.setHint("Search the forgotten archives");
        styleSearch(search);

        ListView list = new ListView(a);
        ArrayAdapter<String> adapter = new DarkAdapter(a, new ArrayList<>(all));
        list.setAdapter(adapter);
        styleList(list);

        LinearLayout panel = new LinearLayout(a);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(18, 14, 18, 12);
        panel.setBackgroundColor(0xFF09090D);
        // ZORTH_BLACK_LIQUID_1
        panel.addView(new LiquidView(a),
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, dp(a, 54)));

        panel.addView(search);
        panel.addView(list, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 850));

        AlertDialog dialog = new AlertDialog.Builder(a)
                .setTitle(catalogue.optString("warning", "The Hidden Library"))
                .setView(panel)
                .setNegativeButton("Close", null)
                .create();

        search.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int st, int count, int after) {}
            public void onTextChanged(CharSequence s, int st, int before, int count) {
                adapter.getFilter().filter(s);
            }
            public void afterTextChanged(Editable e) {}
        });

        list.setOnItemClickListener((parent, view, position, id) -> {
            String name = adapter.getItem(position);
            try {
                for (int i = 0; i < collections.length(); i++) {
                    JSONObject c = collections.getJSONObject(i);
                    if (c.getString("name").equals(name)) {
                        showBooks(a, c);
                        break;
                    }
                }
            } catch (Exception e) {
                Toast.makeText(a, "Could not open collection.",
                        Toast.LENGTH_SHORT).show();
            }
        });

        dialog.show();
        styleDialog(dialog);
    }

    private static void showBooks(Activity a, JSONObject collection)
            throws Exception {
        JSONArray books = collection.optJSONArray("books");
        if (books == null) books = new JSONArray();

        ArrayList<JSONObject> entries = new ArrayList<>();
        ArrayList<String> titles = new ArrayList<>();

        for (int i = 0; i < books.length(); i++) {
            JSONObject book = books.getJSONObject(i);
            entries.add(book);
            titles.add(book.optString("title", "Untitled record"));
        }

        if (entries.isEmpty()) {
            new AlertDialog.Builder(a)
                    .setTitle(collection.optString("name"))
                    .setMessage(collection.optString("description")
                            + "\n\nNo book records have been added yet.")
                    .setPositiveButton("OK", null)
                    .show();
            return;
        }

        EditText search = new EditText(a);
        search.setSingleLine(true);
        search.setHint("Search hidden texts");
        styleSearch(search);

        ListView list = new ListView(a);
        ArrayAdapter<String> adapter = new DarkAdapter(a, new ArrayList<>(titles));
        list.setAdapter(adapter);
        styleList(list);

        LinearLayout panel = new LinearLayout(a);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(18, 14, 18, 12);
        panel.setBackgroundColor(0xFF09090D);
        // ZORTH_BLACK_LIQUID_1
        panel.addView(new LiquidView(a),
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, dp(a, 54)));

        panel.addView(search);
        panel.addView(list, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 850));

        AlertDialog dialog = new AlertDialog.Builder(a)
                .setTitle(collection.optString("name"))
                .setView(panel)
                .setNegativeButton("Back", null)
                .create();

        search.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int st, int count, int after) {}
            public void onTextChanged(CharSequence s, int st, int before, int count) {
                adapter.getFilter().filter(s);
            }
            public void afterTextChanged(Editable e) {}
        });

        list.setOnItemClickListener((parent, view, position, id) -> {
            String title = adapter.getItem(position);
            for (JSONObject book : entries) {
                if (book.optString("title").equals(title)) {
                    showBookDetails(a, book);
                    break;
                }
            }
        });

        dialog.show();
        styleDialog(dialog);
    }



    private static int dp(Activity a, float value) {
        return (int) (value * a.getResources().getDisplayMetrics().density + 0.5f);
    }

    private static final class LiquidView extends View {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private long started;
        private boolean running;

        LiquidView(Activity a) {
            super(a);
            setBackgroundColor(0xFF09090D);
            setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        }

        @Override
        protected void onAttachedToWindow() {
            super.onAttachedToWindow();
            started = android.os.SystemClock.uptimeMillis();
            running = true;
            postInvalidateOnAnimation();
        }

        @Override
        protected void onDetachedFromWindow() {
            running = false;
            removeCallbacks(null);
            super.onDetachedFromWindow();
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            float w = getWidth();
            float h = getHeight();
            if (w <= 0 || h <= 0) return;

            float t = ((android.os.SystemClock.uptimeMillis() - started)
                    % 6000L) / 6000f;

            paint.setStyle(Paint.Style.FILL);
            paint.setColor(0xFF17121B);
            canvas.drawCircle(w * (0.12f + t * 0.7f), h * 0.38f,
                    h * 0.62f, paint);

            paint.setColor(0xFF30202D);
            canvas.drawOval(w * (0.30f - t * 0.12f), h * 0.12f,
                    w * (0.49f + t * 0.08f), h * 0.95f, paint);

            paint.setColor(0xFF67404F);
            paint.setAlpha(115);
            float x1 = w * (0.78f - t * 0.52f);
            float y1 = h * (0.12f + t * 0.72f);
            canvas.drawOval(x1, y1, x1 + h * 0.12f,
                    y1 + h * 0.27f, paint);

            paint.setAlpha(70);
            float x2 = w * (0.22f + t * 0.38f);
            float y2 = h * (0.72f - t * 0.55f);
            canvas.drawOval(x2, y2, x2 + h * 0.07f,
                    y2 + h * 0.18f, paint);

            paint.setAlpha(255);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(dp((Activity) getContext(), 1));
            paint.setColor(0xFF493744);
            canvas.drawLine(0, h - 1, w, h - 1, paint);

            if (running && isAttachedToWindow()) postInvalidateDelayed(70);
        }
    }

    // ZORTH_FORBIDDEN_LIBRARY_STYLE_1
    private static final int VOID = 0xFF09090D;
    private static final int PANEL = 0xFF15121A;
    private static final int SILVER = 0xFFE8E1EF;
    private static final int WINE = 0xFF67404F;

    private static void styleSearch(EditText search) {
        search.setTextColor(SILVER);
        search.setHintTextColor(0xFF918797);
        search.setTextSize(15);
        search.setPadding(20, 14, 20, 14);
        search.setBackground(makeShape(PANEL, WINE, 18));
    }

    private static void styleList(ListView list) {
        list.setBackgroundColor(VOID);
        list.setDivider(new android.graphics.drawable.ColorDrawable(0xFF29232F));
        list.setDividerHeight(1);
        list.setSelector(android.R.color.transparent);
        list.setPadding(0, 8, 0, 8);
    }

    private static android.graphics.drawable.GradientDrawable makeShape(
            int fill, int stroke, float radius) {
        android.graphics.drawable.GradientDrawable shape =
                new android.graphics.drawable.GradientDrawable();
        shape.setColor(fill);
        shape.setCornerRadius(radius);
        shape.setStroke(1, stroke);
        return shape;
    }

    private static void styleDialog(AlertDialog dialog) {
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(
                    makeShape(VOID, 0xFF493744, 24));
        }
        int titleId = dialog.getContext().getResources().getIdentifier(
                "alertTitle", "id", "android");
        android.view.View title = dialog.findViewById(titleId);
        if (title instanceof android.widget.TextView) {
            android.widget.TextView tv = (android.widget.TextView) title;
            tv.setTextColor(0xFFD7C1E5);
            tv.setLetterSpacing(0.04f);
        }
        int messageId = dialog.getContext().getResources().getIdentifier(
                "message", "id", "android");
        android.view.View message = dialog.findViewById(messageId);
        if (message instanceof android.widget.TextView) {
            ((android.widget.TextView) message).setTextColor(SILVER);
        }
    }

    private static final class DarkAdapter extends ArrayAdapter<String> {
        DarkAdapter(Activity activity, ArrayList<String> items) {
            super(activity, android.R.layout.simple_list_item_1, items);
        }

        @Override
        public android.view.View getView(
                int position, android.view.View convertView,
                android.view.ViewGroup parent) {
            android.view.View row =
                    super.getView(position, convertView, parent);
            if (row instanceof android.widget.TextView) {
                android.widget.TextView text =
                        (android.widget.TextView) row;
                text.setTextColor(SILVER);
                text.setTextSize(15);
                text.setPadding(22, 19, 18, 19);
                text.setBackground(makeShape(PANEL, 0xFF302633, 14));
            }
            return row;
        }
    }

    private static void showBookDetails(Activity a, JSONObject book) {
        String bookId = book.optString("id", "");
        boolean unlocked = !bookId.isEmpty()
                && new BookSubscriptionRepository(a)
                        .hasAccess(bookId, Instant.now());

        String details =
                "Author: " + book.optString("author", "Unknown")
                + "\nPeriod: " + book.optString("period", "Unknown")
                + "\nStatus: " + book.optString("status", "Not verified")
                + "\nSource: " + book.optString("source_name", "Unspecified")
                + "\nAccess: " + (unlocked ? "Unlocked" : "Locked — individual subscription required")
                + "\n\n" + book.optString("access_note", "")+ "\n\n" + book.optString("reading_content", "");

        String readingContent = book.optString("reading_content", "").trim();
        AlertDialog.Builder builder = new AlertDialog.Builder(a)
                .setTitle(book.optString("title", "Book details"))
                .setMessage(
                        "Author: " + book.optString("author", "Unknown")
                                + "\nPeriod: " + book.optString("period", "Unknown")
                                + "\nStatus: " + book.optString("status", "Not verified")
                                + "\nSource: " + book.optString("source_name", "Unspecified")
                                + "\nAccess: " + (unlocked
                                ? "Unlocked" : "Locked — individual subscription required")
                                + "\n\n" + book.optString("access_note", "")
                                + (readingContent.isEmpty()
                                ? "\n\nNo reading text is stored in the app for this entry yet."
                                : "\n\nAvailable reading text: " + readingContent.length()
                                + " characters"))
                .setPositiveButton("Close", null);

        if (unlocked && !readingContent.isEmpty()) {
            builder.setNegativeButton("Read in ZØRTH", (d, which) -> {
                android.widget.ScrollView scroll = new android.widget.ScrollView(a);
                android.widget.TextView text = new android.widget.TextView(a);
                text.setText(readingContent);
                text.setTextColor(0xFFEAE7EF);
                text.setTextSize(16);
                text.setPadding(dp(a, 18), dp(a, 16), dp(a, 18), dp(a, 16));
                scroll.addView(text);
                AlertDialog reader = new AlertDialog.Builder(a)
                        .setTitle(book.optString("title", "Reading"))
                        .setView(scroll)
                        .setPositiveButton("Close", null)
                        .create();
                reader.show();
                styleDialog(reader);
            });
        }
        } else if (!unlocked) {
            builder.setNeutralButton("Subscription options", (d, which) ->
                    new AlertDialog.Builder(a)
                            .setTitle("Individual book subscription")
                            .setMessage(
                                    "Book ID: " + (bookId.isEmpty() ? "Missing" : bookId)
                                    + "\n\nThis book requires its own subscription. "
                                    + "Checkout and verified payment activation are not "
                                    + "connected yet, so access has not been granted.")
                            .setPositiveButton("OK", null)
                            .show());
        }

        AlertDialog detailDialog = builder.create();
        detailDialog.show();
        styleDialog(detailDialog);
    }
}
