package at.sljivic.aegis

import aegis.composeapp.generated.resources.Res
import aegis.composeapp.generated.resources.compose_multiplatform
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import java.io.BufferedReader
import java.io.File
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import at.sljivic.aegis.PolicyRule

data class SandboxingOptions(val syscall_restrictions: ArrayList<Int>)

enum class OperationType {
    File,
    Network,
    ProcessManagement,
    Unclassified
}

data class Syscall(val name: String, val type: OperationType)

expect fun syscallNumToSyscall(num: Int): Syscall
expect fun syscallNameToNum(name: String): Int
expect fun getNumSyscalls(): Int
expect fun getSyscallsOfType(type: OperationType): ArrayList<String>


expect fun traceExecutable(
        executablePath: String,
        args: List<String>,
        timeoutSeconds: Long,
        sandbox: SandboxingOptions
): String


enum class Action {
    Allow, Deny
}

data class PolicyRule(val action: Action, val syscall_name: String) 

fun parsePolicyFile(path: String): SandboxingOptions {
    val bufferedReader: BufferedReader = File(path).bufferedReader()
    val policyContent = bufferedReader.use { it.readText() }
    val policyLines = policyContent.split("\n");

    val rules = ArrayList<PolicyRule>();
    println("Rule file:");
    println(policyContent);
    println("===========\n")

    for (rule in policyLines) {
        if(rule == "" || rule.isBlank()) {
            continue
        }
        println("Looking at rule");
        println(rule);
        
        val parts = rule.split(" ")
        if(parts.size != 2) {
            println("Rule file malformed. Part size is not 2")
        }
        if(parts.get(0) == "ALLOW")
            rules.add(PolicyRule(Action.Allow, parts.get(1)));
        else if(parts.get(0) == "DENY")
            rules.add(PolicyRule(Action.Deny, parts.get(1)));
        else 
           println("Rule file malformed. Action neither allow nor deny")
    }
    if(rules.get(0).syscall_name != "DEFAULT") {
        println("Rule file malformed. No default action")
    }
   val sandbox = SandboxingOptions(ArrayList<Int>());
    if(rules.get(0).action == Action.Allow) {
        // the rest is all denies - TODO check explciitly
        for(rule in rules) {
            if(rule.syscall_name != "DEFAULT") // todo: how to skip in kotlin
            {
                sandbox.syscall_restrictions.add(syscallNameToNum(rule.syscall_name));
            }
        }
    }
    else {
        // we need to deny all not explicitly mentioned
        // assume the numbers are sorted for now
        var curr_num = 0
        for(rule in rules) {
            if(rule.syscall_name != "DEFAULT") // todo: how to skip in kotlin
            {
                val policy_num = syscallNameToNum(rule.syscall_name)
                while(curr_num != policy_num && curr_num < getNumSyscalls()) {
                    sandbox.syscall_restrictions.add(curr_num);
                    curr_num++;
                }
                if(curr_num == policy_num) {
                    curr_num++;
                }
            }
        }
    }
 
    
    return sandbox;
}

fun createPolicy(restrictions: ArrayList<OperationType>, path: String) {
    var output_policy = "ALLOW DEFAULT";
    for (restriction in restrictions) {
        var syscall_names = getSyscallsOfType(restriction);
        for (syscall in syscall_names) {
            output_policy += "\nDENY " + syscall;
        }
    }
    File(path).writeText(output_policy);
}



fun getSyscallList(): ArrayList<Syscall> {

    val sandbox_config = parsePolicyFile("/tmp/HackaTUM/policy.aegis");
    val nums =
            traceExecutable("id", listOf("."), 60, sandbox_config)
                    .split("\n")
    var syscalls_in_order = ArrayList<Syscall>()
    for (num in nums) {
        //   println("to int: $num");
        val intNum = num.toInt()
        val syscall = syscallNumToSyscall(intNum)
        // println("Syscall was: $syscall")
        syscalls_in_order.add(syscall)
    }
    return syscalls_in_order
}

@Composable
@Preview
fun App() {
    MaterialTheme {
        var showContent by remember { mutableStateOf(false) }
        Column(
                modifier =
                        Modifier.background(MaterialTheme.colorScheme.primaryContainer)
                                .safeContentPadding()
                                .fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Button(onClick = { showContent = !showContent }) { Text("Click me!") }
            AnimatedVisibility(showContent) {
                val greeting = remember { Greeting().greet() }
                println(getSyscallList())
                val file_only_policy = ArrayList<OperationType>(listOf(OperationType.Network, OperationType.ProcessManagement));

                createPolicy(file_only_policy, "/tmp/HackaTUM/gen_policy.aegis")
                Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Image(painterResource(Res.drawable.compose_multiplatform), null)
                    Text("Compose: $greeting")
                }
            }
        }
    }

}
