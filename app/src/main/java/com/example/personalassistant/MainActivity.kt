package com.example.personalassistant

import Task
import TaskAdapter
import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.ai.client.generativeai.GenerativeModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken




class MainActivity : AppCompatActivity() {
    private lateinit var editTextApiKey: EditText
    private lateinit var btnSaveApiKey: Button
    private lateinit var btnSubmit: Button
    private lateinit var textViewOutput: TextView
    private lateinit var taskAdapter: TaskAdapter
    private val tasks: MutableList<Task> = mutableListOf(Task())
    private lateinit var recyclerViewTasks: RecyclerView
    private lateinit var spinnerUserType: Spinner


    private val sharedPref by lazy {
        getSharedPreferences("AppPreferences", Context.MODE_PRIVATE)
    }

    private fun saveScheduleToPreferences(scheduleTasks: List<ScheduleTask>) {
        val json = Gson().toJson(scheduleTasks) // Convert scheduleTasks to JSON string
        with(sharedPref.edit()) {
            putString("SCHEDULE_TASKS", json) // Save JSON string in SharedPreferences
            apply()
        }
    }

    private fun loadScheduleFromPreferences(): List<ScheduleTask> {
        val json = sharedPref.getString("SCHEDULE_TASKS", null)
        val recyclerView = findViewById<RecyclerView>(R.id.recyclerViewSchedule)
        return if (json != null) {
            recyclerView.visibility= View.VISIBLE
            val type = object : TypeToken<List<ScheduleTask>>() {}.type
            Gson().fromJson(json, type) // Convert JSON string back to List<ScheduleTask>
        } else {
            emptyList() // Return an empty list if no schedule was saved
        }
    }



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activitymain)

        editTextApiKey = findViewById(R.id.editTextApiKey)
        btnSaveApiKey = findViewById(R.id.btnSaveApiKey)
        btnSubmit = findViewById(R.id.btnSubmit)
        recyclerViewTasks = findViewById(R.id.recyclerViewTasks)
        spinnerUserType = findViewById(R.id.spinnerUserType)


        val adapter = ArrayAdapter.createFromResource(
            this,
            R.array.user_types,
            R.layout.selected_spinner_item // Use the custom layout here
        )
        adapter.setDropDownViewResource(R.layout.spinner_item) // Also set it for the dropdown view
        spinnerUserType.adapter = adapter
        // Load saved API key if available
        loadApiKey()

        // Initialize RecyclerView with TaskAdapter
        taskAdapter = TaskAdapter(tasks,this)
        recyclerViewTasks.adapter = taskAdapter
        recyclerViewTasks.layoutManager = LinearLayoutManager(this)

        val savedSchedule = loadScheduleFromPreferences()
        if (savedSchedule.isNotEmpty()) {
            val recyclerView: RecyclerView = findViewById(R.id.recyclerViewSchedule)
            val scheduleAdapter = ScheduleAdapter(this, savedSchedule)
            recyclerView.adapter = scheduleAdapter
            recyclerView.layoutManager = LinearLayoutManager(this)
        }

        val addTaskButton: Button = findViewById(R.id.addTaskButton)
        addTaskButton.setOnClickListener {
            // Add a new empty task to the list and notify the adapter
            taskAdapter.addTask(Task())  // Add an empty task
        }


        // Save API key on button click
        btnSaveApiKey.setOnClickListener {
            val apiKey = editTextApiKey.text.toString().trim()
            if (apiKey.isNotEmpty()) {
                saveApiKey(apiKey)
                textViewOutput.text = "API Key saved!"
            } else {
                textViewOutput.text = "Please enter a valid API Key."
            }
        }

        // Submit button to generate schedule
        btnSubmit.setOnClickListener {
            val recyclerView = findViewById<RecyclerView>(R.id.recyclerViewSchedule)
            recyclerView.visibility= View.VISIBLE
            generateSchedule() }
    }

    private fun loadApiKey() {
        val savedApiKey = sharedPref.getString("API_KEY", null)
        if (savedApiKey != null) {
            editTextApiKey.setText(savedApiKey)
        }
    }

    private fun saveApiKey(apiKey: String) {
        with(sharedPref.edit()) {
            putString("API_KEY", apiKey)
            apply()
        }
    }

    private fun parseScheduleText(scheduleText: String): List<ScheduleTask> {
        val scheduleTasks = mutableListOf<ScheduleTask>()

        scheduleText.lines().forEach { line ->
            val columns = line.split("|").map { it.trim() }.filter { it.isNotEmpty() }
            if (columns.size == 2) { // Assuming two columns: time and task
                val time = columns[0]
                val task = columns[1]
                scheduleTasks.add(ScheduleTask(time, task))
            }
        }
        return scheduleTasks
    }

    private fun generateSchedule() {
        val apiKey = sharedPref.getString("API_KEY", null)
        if (apiKey.isNullOrEmpty()) {
            textViewOutput.text = "Please enter and save a valid API Key."
            return
        }

        val userType = spinnerUserType.selectedItem.toString()
        val tasks = taskAdapter.tasks.filter { it.taskName.isNotEmpty() }
        val tasksDescription = tasks.joinToString("\n") { "${it.taskName}: ${it.timeTaken} mins, ${it.energyType}" }

        val prompt = "Imagine you are my Personnel assistant whose sole purpose is to give me a daily schedule for a $userType person as I have the Highest form of energy during that time according to my chronotype.The Time should be given in 24 hour format as I'll be using the data for further computation and any other type will cause errors .I want you to give a schedule for performing the following tasks.The tasks Are well structured saying the name the duration required to finish it and the energy consumption the output is also expected to be structured which should be arranged according to my energy levels which means if i am a morning type person then I want to be doing my High energy tasks in the morning my tasks are as follows:\n $tasksDescription \n here High Energy, Low Energy and Moderate Energy are not tasks but instead are the description of tasks mentioned before them it includes the task at hand the amount of time it'd take and the amount of energy required to do it  also include timely breaks so that I don't get burnt out and most importantly remember I want the output to be of the following format: \n|StartTime|Tasks| \nin a tabular form I don't want any other kinds fo response from you or anything extra since I am using this as prompt for my other calculations and anyother addtional data will ruin it so please just stick to the format \n Example of what output should look like:|StartTime|Task|\n|7:00|Breakfast|\n|7:30|Study session|\n|19:00|break|\n|22:00|sleep|\n again I want the output to be of eactly this format as I'll be using the time for other calculations and any other format will cause exceptions and eventually crash"
        val generativeModel = GenerativeModel(
            modelName = "gemini-1.5-flash",
            apiKey = apiKey
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
            }
        }


        lifecycleScope.launch {
            try {
                // Run the network request in a background thread
                val response = withContext(Dispatchers.IO) {
                    generativeModel.generateContent(prompt)
                }
                // Update UI with the result on the main thread
                val scheduleText = response.text ?:" "
                val scheduleTasks = parseScheduleText(scheduleText)

                saveScheduleToPreferences(scheduleTasks)

                val recyclerView: RecyclerView = findViewById(R.id.recyclerViewSchedule)
                val scheduleAdapter = ScheduleAdapter(this@MainActivity,scheduleTasks)
                recyclerView.adapter = scheduleAdapter
                recyclerView.layoutManager = LinearLayoutManager(this@MainActivity)
                recyclerView.setNestedScrollingEnabled(false);

            } catch (e: Exception) {
                // Handle the exception, e.g., show error message
            }
        }
    }

}
