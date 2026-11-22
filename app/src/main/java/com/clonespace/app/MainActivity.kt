package com.clonespace.app

import android.app.Activity
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {

    private val REQ_PROVISION = 1001

    private lateinit var dpm: DevicePolicyManager
    private lateinit var admin: ComponentName
    private lateinit var status: TextView
    private lateinit var action: Button
    private lateinit var list: ListView

    private data class Item(val label: String, val pkg: String, val installed: Boolean)

    private val isInSpace: Boolean
        get() = dpm.isProfileOwnerApp(packageName)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        dpm = getSystemService(DevicePolicyManager::class.java)
        admin = ComponentName(this, AdminReceiver::class.java)
        buildUi()
        refresh()
    }

    private fun buildUi() {
        val pad = (16 * resources.displayMetrics.density).toInt()
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(pad, pad, pad, pad)
        }
        status = TextView(this).apply { textSize = 16f; setPadding(0, 0, 0, pad) }
        action = Button(this)
        list = ListView(this)
        root.addView(status)
        root.addView(action, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        root.addView(list, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
        setContentView(root)
    }

    private fun refresh() {
        if (isInSpace) showSpaceUi() else showSetupUi()
    }

    // ---------- Main profile: create the clone space ----------

    private fun showSetupUi() {
        list.visibility = View.GONE
        status.text = "Create a Imad Clone: a second, isolated copy of your apps " +
                "(separate accounts, data and logins). After setup, open the " +
                "Imad Clone app marked with the briefcase badge."
        action.text = "Create Imad Clone"
        action.setOnClickListener { startProvisioning() }
    }

    private fun startProvisioning() {
        if (!dpm.isProvisioningAllowed(DevicePolicyManager.ACTION_PROVISION_MANAGED_PROFILE)) {
            Toast.makeText(this,
                "This phone doesn't allow creating a second space (already exists or blocked).",
                Toast.LENGTH_LONG).show()
            return
        }
        val intent = Intent(DevicePolicyManager.ACTION_PROVISION_MANAGED_PROFILE)
            .putExtra(DevicePolicyManager.EXTRA_PROVISIONING_DEVICE_ADMIN_COMPONENT_NAME, admin)
        if (intent.resolveActivity(packageManager) != null) {
            @Suppress("DEPRECATION")
            startActivityForResult(intent, REQ_PROVISION)
        } else {
            Toast.makeText(this, "Managed profiles aren't supported on this device.",
                Toast.LENGTH_LONG).show()
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQ_PROVISION && resultCode != RESULT_OK) {
            Toast.makeText(this, "Setup cancelled", Toast.LENGTH_SHORT).show()
        }
    }

    // ---------- Inside the space: choose apps to clone ----------

    private fun showSpaceUi() {
        list.visibility = View.VISIBLE
        status.text = "Tap an app to clone it into this space. Cloned apps (✓) open here " +
                "and also show up on your home screen with a briefcase badge."
        action.text = "Enable Google Play in this space"
        action.setOnClickListener {
            try {
                dpm.enableSystemApp(admin, "com.android.vending")
                dpm.enableSystemApp(admin, "com.google.android.gms")
                Toast.makeText(this, "Google Play enabled", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(this, "Couldn't enable Play: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
        loadApps()
    }

    private fun loadApps() {
        val pm = packageManager
        val items = pm.getInstalledApplications(PackageManager.MATCH_UNINSTALLED_PACKAGES)
            .filter { it.packageName != packageName }
            .filter { it.flags and ApplicationInfo.FLAG_SYSTEM == 0 ||
                      it.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP != 0 }
            .map {
                Item(
                    label = it.loadLabel(pm).toString(),
                    pkg = it.packageName,
                    installed = it.flags and ApplicationInfo.FLAG_INSTALLED != 0
                )
            }
            .sortedBy { it.label.lowercase() }

        list.adapter = object : ArrayAdapter<Item>(this, android.R.layout.simple_list_item_2, android.R.id.text1, items) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val v = super.getView(position, convertView, parent)
                val item = getItem(position)!!
                v.findViewById<TextView>(android.R.id.text1).apply {
                    text = (if (item.installed) "✓ " else "") + item.label
                    setTypeface(null, if (item.installed) Typeface.BOLD else Typeface.NORMAL)
                }
                v.findViewById<TextView>(android.R.id.text2).text =
                    if (item.installed) "Cloned: tap to open" else "Tap to clone"
                return v
            }
        }
        list.setOnItemClickListener { _, _, pos, _ ->
            val item = items[pos]
            if (item.installed) openApp(item.pkg) else cloneApp(item.pkg)
        }
    }

    private fun cloneApp(pkg: String) {
        try {
            val ok = dpm.installExistingPackage(admin, pkg)
            Toast.makeText(this,
                if (ok) "Cloned!" else "Android refused to clone this app",
                Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
        }
        list.postDelayed({ loadApps() }, 800)
    }

    private fun openApp(pkg: String) {
        val intent = packageManager.getLaunchIntentForPackage(pkg)
        if (intent != null) startActivity(intent)
        else Toast.makeText(this, "No launchable screen for this app", Toast.LENGTH_SHORT).show()
    }
}
