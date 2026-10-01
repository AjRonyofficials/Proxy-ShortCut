package com.example

import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class AppSelectActivity : AppCompatActivity() {

    private lateinit var rvApps: RecyclerView
    private var activeProfile: ProxyProfile? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_app_select)

        activeProfile = ProfileStorage.getActiveProfile(this)
        rvApps = findViewById(R.id.rvApps)
        rvApps.layoutManager = LinearLayoutManager(this)

        val pm = packageManager
        val apps = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.getInstalledApplications(PackageManager.ApplicationInfoFlags.of(PackageManager.GET_META_DATA.toLong()))
        } else {
            @Suppress("DEPRECATION")
            pm.getInstalledApplications(PackageManager.GET_META_DATA)
        }.filter { (it.flags and ApplicationInfo.FLAG_SYSTEM) == 0 }

        rvApps.adapter = AppAdapter(apps, pm, activeProfile?.selectedApps ?: HashSet()) {
            activeProfile?.let { prof ->
                ProfileStorage.saveProfiles(this, listOf(prof))
            }
        }
    }

    class AppAdapter(
        private val list: List<ApplicationInfo>,
        private val pm: PackageManager,
        private val selected: HashSet<String>,
        private val onChange: () -> Unit
    ) : RecyclerView.Adapter<AppAdapter.ViewHolder>() {

        class ViewHolder(v: View) : RecyclerView.ViewHolder(v) {
            val icon: ImageView = v.findViewById(R.id.imgAppIcon)
            val name: TextView = v.findViewById(R.id.txtAppName)
            val cb: CheckBox = v.findViewById(R.id.cbApp)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_app_select, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val app = list[position]
            holder.name.text = app.loadLabel(pm)
            holder.icon.setImageDrawable(app.loadIcon(pm))
            holder.cb.isChecked = selected.contains(app.packageName)

            holder.cb.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) selected.add(app.packageName) else selected.remove(app.packageName)
                onChange()
            }
        }

        override fun getItemCount() = list.size
    }
}
