package com.tuautoradio.launcher

import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.GridView
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var tvClock: TextView
    private lateinit var tvClockBottom: TextView
    private lateinit var tvDate: TextView
    private lateinit var gridApps: GridView

    private val handler = Handler(Looper.getMainLooper())
    private val clockRunnable = object : Runnable {
        override fun run() {
            updateClock()
            handler.postDelayed(this, 1000)
        }
    }

    // Paquetes NWD mapeados en tu autoradio
    private val PKG_RADIO = "com.nwd.radio"
    private val PKG_MUSIC = "com.nwd.android.music.ui"
    private val PKG_PHONE = "com.nwd.android.phone"
    private val PKG_CAMERA = "com.nwd.backcar"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvClock = findViewById(R.id.tvClock)
        tvClockBottom = findViewById(R.id.tvClockBottom)
        tvDate = findViewById(R.id.tvDate)
        gridApps = findViewById(R.id.gridApps)

        findViewById<View>(R.id.btnRadio).setOnClickListener { launchPackage(PKG_RADIO) }
        findViewById<View>(R.id.btnMusic).setOnClickListener { launchPackage(PKG_MUSIC) }
        findViewById<View>(R.id.btnPhone).setOnClickListener { launchPackage(PKG_PHONE) }
        findViewById<View>(R.id.btnCamera).setOnClickListener { launchPackage(PKG_CAMERA) }

        loadApps()
    }

    override fun onResume() {
        super.onResume()
        handler.post(clockRunnable)
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(clockRunnable)
    }

    private fun updateClock() {
        val now = Date()
        val timeFmt = SimpleDateFormat("HH:mm", Locale.getDefault())
        val dateFmt = SimpleDateFormat("EEEE d 'de' MMMM", Locale("es", "ES"))
        tvClock.text = timeFmt.format(now)
        tvClockBottom.text = timeFmt.format(now)
        tvDate.text = dateFmt.format(now).replaceFirstChar { it.uppercase() }
    }

    private fun launchPackage(packageName: String) {
        val intent = packageManager.getLaunchIntentForPackage(packageName)
        if (intent != null) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
        } else {
            Toast.makeText(this, "No se encontró: $packageName", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadApps() {
        val pm = packageManager
        val mainIntent = Intent(Intent.ACTION_MAIN, null)
        mainIntent.addCategory(Intent.CATEGORY_LAUNCHER)

        val resolveInfos: List<ResolveInfo> = pm.queryIntentActivities(mainIntent, 0)
            .filter { it.activityInfo.packageName != packageName }
            .sortedBy { it.loadLabel(pm).toString().lowercase(Locale.getDefault()) }

        gridApps.adapter = AppsAdapter(resolveInfos, pm)

        gridApps.setOnItemClickListener { _, _, position, _ ->
            val info = resolveInfos[position].activityInfo
            val launchIntent = Intent()
                .setClassName(info.packageName, info.name)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            try {
                startActivity(launchIntent)
            } catch (e: Exception) {
                Toast.makeText(this, "No se pudo abrir la app", Toast.LENGTH_SHORT).show()
            }
        }
    }

    inner class AppsAdapter(
        private val apps: List<ResolveInfo>,
        private val pm: PackageManager
    ) : BaseAdapter() {

        override fun getCount(): Int = apps.size
        override fun getItem(position: Int): Any = apps[position]
        override fun getItemId(position: Int): Long = position.toLong()

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val view = convertView ?: LayoutInflater.from(this@MainActivity)
                .inflate(R.layout.item_app, parent, false)

            val info = apps[position]
            val icon = view.findViewById<ImageView>(R.id.ivIcon)
            val label = view.findViewById<TextView>(R.id.tvLabel)

            icon.setImageDrawable(info.loadIcon(pm))
            label.text = info.loadLabel(pm)

            return view
        }
    }
}
