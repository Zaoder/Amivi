package gr.teo.amoivi;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;
import android.view.View;
import android.widget.RemoteViews;

import org.json.JSONObject;

import java.util.Calendar;
import java.util.Locale;

public class AmoiviWidget extends AppWidgetProvider {

    static final String ACTION_TICK = "gr.teo.amoivi.TICK";
    private static final Locale GR = new Locale("el", "GR");

    @Override
    public void onUpdate(Context ctx, AppWidgetManager mgr, int[] ids) {
        render(ctx, mgr, ids);
    }

    @Override
    public void onReceive(Context ctx, Intent intent) {
        super.onReceive(ctx, intent);
        if (ACTION_TICK.equals(intent.getAction())) updateAll(ctx);
    }

    @Override
    public void onDisabled(Context ctx) {
        cancelTick(ctx);
    }

    static void updateAll(Context ctx) {
        AppWidgetManager mgr = AppWidgetManager.getInstance(ctx);
        int[] ids = mgr.getAppWidgetIds(new ComponentName(ctx, AmoiviWidget.class));
        if (ids.length == 0) { cancelTick(ctx); return; }
        render(ctx, mgr, ids);
    }

    private static String money(double v) {
        return String.format(GR, "%,.2f", Math.round(v * 100) / 100.0);
    }

    private static String hhmm(long ts) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(ts);
        return String.format(GR, "%02d:%02d", c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE));
    }

    private static void render(Context ctx, AppWidgetManager mgr, int[] ids) {
        long start = 0, end = 0;
        double rate = 8, exp = 0;
        try {
            String raw = ctx.getSharedPreferences(MainActivity.PREFS, Context.MODE_PRIVATE)
                    .getString(MainActivity.KEY_STATE, "");
            if (raw != null && !raw.isEmpty()) {
                JSONObject o = new JSONObject(raw);
                if (!o.isNull("start")) start = o.optLong("start", 0);
                if (!o.isNull("end")) end = o.optLong("end", 0);
                rate = o.optDouble("rate", 8);
                exp = o.optDouble("exp", 0);
                if (Double.isNaN(rate)) rate = 8;
                if (Double.isNaN(exp)) exp = 0;
            }
        } catch (Exception ignored) { }

        long now = System.currentTimeMillis();
        boolean running = start > 0 && end == 0;

        RemoteViews v = new RemoteViews(ctx.getPackageName(), R.layout.widget);

        Intent open = new Intent(ctx, MainActivity.class);
        open.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent pi = PendingIntent.getActivity(ctx, 0, open,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        v.setOnClickPendingIntent(R.id.root, pi);

        if (start == 0) {
            v.setTextViewText(R.id.state, "Ανενεργό");
            v.setTextColor(R.id.state, 0xFF8B919E);
            v.setTextViewText(R.id.amount, "0,00 €");
            v.setChronometer(R.id.elapsed, SystemClock.elapsedRealtime(), null, false);
            v.setTextViewText(R.id.exp, "Πάτα για έναρξη");
            v.setViewVisibility(R.id.net, View.GONE);
            cancelTick(ctx);
        } else {
            long stop = running ? now : end;
            long ms = Math.max(0, stop - start);
            double earned = ms / 3600000.0 * rate;

            if (running) {
                v.setTextViewText(R.id.state, "● Σε βάρδια · από " + hhmm(start));
                v.setTextColor(R.id.state, 0xFF3DDC97);
            } else {
                v.setTextViewText(R.id.state, "Ολοκληρώθηκε · " + hhmm(start) + "–" + hhmm(end));
                v.setTextColor(R.id.state, 0xFFFFB547);
            }
            v.setTextViewText(R.id.amount, money(earned) + " €");
            // Το ρολόι τρέχει μόνο του κάθε δευτερόλεπτο, χωρίς κόστος μπαταρίας
            v.setChronometer(R.id.elapsed, SystemClock.elapsedRealtime() - ms, null, running);
            v.setTextViewText(R.id.exp, "Έξοδα −" + money(exp) + " €");
            v.setViewVisibility(R.id.net, View.VISIBLE);
            v.setTextViewText(R.id.net, "Καθαρά " + money(earned - exp) + " €");

            if (running) scheduleTick(ctx); else cancelTick(ctx);
        }

        mgr.updateAppWidget(ids, v);
    }

    private static PendingIntent tickIntent(Context ctx) {
        Intent i = new Intent(ctx, AmoiviWidget.class);
        i.setAction(ACTION_TICK);
        return PendingIntent.getBroadcast(ctx, 1, i,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    /** Ανανέωση του ποσού στην αρχή κάθε λεπτού (δεν ξυπνάει το κινητό όταν κοιμάται). */
    private static void scheduleTick(Context ctx) {
        AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;
        long now = System.currentTimeMillis();
        long next = (now / 60000 + 1) * 60000 + 500;
        am.set(AlarmManager.RTC, next, tickIntent(ctx));
    }

    private static void cancelTick(Context ctx) {
        AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
        if (am != null) am.cancel(tickIntent(ctx));
    }
}
