import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Spinner
import androidx.recyclerview.widget.RecyclerView
import com.example.personalassistant.R

data class Task(var taskName: String = "", var timeTaken: Int = 0, var energyType: String = "High")

class TaskAdapter(val tasks: MutableList<Task>, val context: Context) : RecyclerView.Adapter<TaskAdapter.TaskViewHolder>() {

    private var shouldFocusNewTask = false
    inner class TaskViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val taskNameEditText: EditText = itemView.findViewById(R.id.editTextTaskName)
        val timeTakenEditText: EditText = itemView.findViewById(R.id.editTextTimeTaken)
        val energyTypeSpinner: Spinner = itemView.findViewById(R.id.spinnerEnergyType)

        fun bind(task: Task) {
            // Set text fields
            taskNameEditText.setText(task.taskName)
            timeTakenEditText.setText(if (task.timeTaken > 0) task.timeTaken.toString() else "")

            // Set up energy type spinner with options if not set
            val energyAdapter = ArrayAdapter(
                itemView.context,
                android.R.layout.simple_spinner_item,
                itemView.context.resources.getStringArray(R.array.energy_types) // Ensure this array is defined in res/values/strings.xml
            )
            energyAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            energyTypeSpinner.adapter = energyAdapter

            val energyTypeAdapter = ArrayAdapter.createFromResource(
                context,
                R.array.energy_types, // Assuming you have a string array resource for energy types
                R.layout.selected_spinner_item // Custom layout with text color
            )
            energyTypeAdapter.setDropDownViewResource(R.layout.spinner_item)

            energyTypeSpinner.adapter = energyTypeAdapter
            // Set spinner selection
            energyTypeSpinner.setSelection(energyAdapter.getPosition(task.energyType))

            // TextWatchers for EditText fields
            taskNameEditText.addTextChangedListener(object : TextWatcher {
                override fun afterTextChanged(s: Editable?) {
                    task.taskName = s.toString()
                }
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            })

            timeTakenEditText.addTextChangedListener(object : TextWatcher {
                override fun afterTextChanged(s: Editable?) {
                    task.timeTaken = s.toString().toIntOrNull() ?: 0
                }
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            })

            // Spinner listener
            energyTypeSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                    task.energyType = parent.getItemAtPosition(position).toString()
                }
                override fun onNothingSelected(parent: AdapterView<*>) {}
            }

            if (shouldFocusNewTask && bindingAdapterPosition == tasks.size - 1) {
                taskNameEditText.requestFocus()
                shouldFocusNewTask = false // Reset the flag after setting focus
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_task, parent, false)
        return TaskViewHolder(view)
    }

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        holder.bind(tasks[position])
    }

    override fun getItemCount(): Int = tasks.size

    // Function to add a new task
    fun addTask(task: Task) {
        tasks.add(task)
        notifyItemInserted(tasks.size - 1)
        shouldFocusNewTask = true
    }
}
