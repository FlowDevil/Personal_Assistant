package com.example.personalassistant

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale


data class ScheduleTask(val time: String, val task: String)

class ScheduleAdapter(private val context: Context, val scheduleTasks: List<ScheduleTask>) :
    RecyclerView.Adapter<ScheduleAdapter.ScheduleViewHolder>() {

    inner class ScheduleViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textViewTime: TextView = itemView.findViewById(R.id.textViewTime)
        val textViewTask: TextView = itemView.findViewById(R.id.textViewTask)
        val buttonSetAlarm: Button = itemView.findViewById(R.id.buttonSetAlarm)

        fun bind(scheduleTask: ScheduleTask) {
            textViewTime.text = scheduleTask.time
            textViewTask.text = scheduleTask.task

            if (scheduleTask.time.contains("-")) {
                textViewTime.visibility = View.GONE
            }

            if (scheduleTask.task.contains("-")) {
                textViewTask.visibility = View.GONE
            }

            // Check if time contains "Time" or "-" and hide buttonSetAlarm if true
            if (scheduleTask.time.contains("Time") || scheduleTask.time.contains("-")) {
                buttonSetAlarm.visibility = View.GONE
            } else {
                buttonSetAlarm.visibility = View.VISIBLE

                // Handle Set Alarm button click
                buttonSetAlarm.setOnClickListener {
                    // Call a method to set an alarm for the task
                    setAlarmForTask(context, scheduleTask.time, scheduleTask.task)
                }
            }
        }
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ScheduleViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_schedule_task, parent, false)
        return ScheduleViewHolder(view)
    }

    override fun onBindViewHolder(holder: ScheduleViewHolder, position: Int) {
        holder.bind(scheduleTasks[position])
    }

    override fun getItemCount(): Int = scheduleTasks.size

    fun setAlarmForTask(context: Context, taskTime: String, taskName: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!context.getSystemService(AlarmManager::class.java).canScheduleExactAlarms()) {
                Toast.makeText(context, "Permission required to set exact alarms.", Toast.LENGTH_LONG).show()
                return
            }
        }

        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

        try {
            val taskTimeDate: Date = timeFormat.parse(taskTime) ?: return

            // Create a Calendar instance and set it to today's date with the task time
            val calendar = Calendar.getInstance().apply {
                timeInMillis = System.currentTimeMillis()  // Set to current date and time
                val taskCalendar = Calendar.getInstance().apply {
                    time = taskTimeDate
                }
                set(Calendar.HOUR_OF_DAY, taskCalendar.get(Calendar.HOUR_OF_DAY))
                set(Calendar.MINUTE, taskCalendar.get(Calendar.MINUTE))
                set(Calendar.SECOND, 0)
            }

            if (calendar.timeInMillis <= System.currentTimeMillis()) {
                calendar.add(Calendar.DAY_OF_YEAR, 1) // Set to next day if time already passed today
            }

            val intent = Intent(context, AlarmReceiver::class.java).apply {
                putExtra("TASK_NAME", taskName)
            }

            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val pendingIntent = PendingIntent.getBroadcast(
                context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent)
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent)
            }

            Toast.makeText(context, "Alarm set for: $taskName at $taskTime", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(context, "Error setting alarm: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

}
