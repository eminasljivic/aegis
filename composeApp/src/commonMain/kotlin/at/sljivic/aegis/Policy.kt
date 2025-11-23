package at.sljivic.aegis



import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.compose.AppTheme
import java.io.BufferedReader
import java.io.File
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import at.sljivic.aegis.PolicyRule
import at.sljivic.aegis.syscallNumToSyscall
import at.sljivic.aegis.getSyscallList
import at.sljivic.aegis.Action
import at.sljivic.aegis.syscallNameToNum
import kotlin.concurrent.thread

data class SandboxingOptions(val syscall_restrictions: ArrayList<Int>, val syscall_restrictions_stage_2: ArrayList<Int>, var condition: Int)

enum class Action {
    Allow, Deny, When
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
        if (rule == "" || rule.isBlank()) {
            continue
        }
        println("Looking at rule");
        println(rule);

        val parts = rule.split(" ")
        if (parts.size != 2) {
           throw Exception("Policy malformed, Part size is not 2")
        }
        if (parts.get(0) == "ALLOW")
            rules.add(PolicyRule(Action.Allow, parts.get(1)));
        else if (parts.get(0) == "DENY")
            rules.add(PolicyRule(Action.Deny, parts.get(1)));
        else if(parts.get(0) == "WHEN") 
            rules.add(PolicyRule(Action.When, parts.get(1)));
        else
           throw Exception("Policy malformed, Action neither allow nor deny")
    }
    if (rules.get(0).syscall_name != "DEFAULT") {
       throw Exception("Policy malformed, No default action")
    }
    var sandbox = SandboxingOptions(ArrayList<Int>(), ArrayList<Int>(),0)
    var stage_2 = false;
    if (rules.get(0).action == Action.Allow) {
        // the rest is all denies - TODO check explciitly
        for (rule in rules) {
            if(!stage_2 && rule.action == Action.When) {
                stage_2 = true;
                sandbox.condition = syscallNameToNum(rule.syscall_name)
                continue
            }
            if (rule.syscall_name != "DEFAULT") // todo: how to skip in kotlin
            {
                if(!stage_2)
                sandbox.syscall_restrictions.add(syscallNameToNum(rule.syscall_name));
                else  
                sandbox.syscall_restrictions_stage_2.add(syscallNameToNum(rule.syscall_name));
            }
        }
    } else {
        // we need to deny all not explicitly mentioned
        // assume the numbers are sorted for now

        val sorted = buildList {
        if (rules.isNotEmpty()) add(rules.first())
         addAll(
        rules.drop(1).sortedBy { syscallNameToNum(it.syscall_name) }
        )
        }

        var curr_num = 0
        for (rule in sorted) {
            if(rule.action == Action.When) {
                println("When not supported in deny default mode. Sorry!\n");
                throw Exception("Policy malformed")
            }
            if (rule.syscall_name != "DEFAULT") // todo: how to skip in kotlin
            {
                val policy_num = syscallNameToNum(rule.syscall_name)
                while (curr_num != policy_num && curr_num < getNumSyscalls()) {
                    if(!stage_2) {
                        sandbox.syscall_restrictions.add(curr_num);
                    } else {
                         sandbox.syscall_restrictions_stage_2.add(curr_num);
                     }
                    curr_num++;
                }
                if (curr_num == policy_num) {
                    curr_num++;
                }
            }
        }
    } 
    return sandbox
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
