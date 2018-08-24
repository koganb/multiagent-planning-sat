package org.agreement_technologies.service.map_parser;

import org.agreement_technologies.common.map_parser.AgentList;
import org.agreement_technologies.common.map_parser.PDDLParser;
import org.agreement_technologies.common.map_parser.Task;
import org.agreement_technologies.service.map_parser.SynAnalyzer.Symbol;
import org.agreement_technologies.service.map_parser.TaskImp.MetricImp;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Oscar
 */
public class MAPDDLParserImp implements PDDLParser {

    private final ArrayList<TaskImp.Variable> privatePredicates;
    private final ArrayList<TaskImp.Value> privateObjects;

    public MAPDDLParserImp() {
        privatePredicates = new ArrayList<TaskImp.Variable>();
        privateObjects = new ArrayList<TaskImp.Value>();
    }

    @Override
    public Task parseDomain(String domainFile) throws ParseException, IOException {
        String content = readToString(domainFile);
        SynAnalyzer syn = new SynAnalyzer(content);
        TaskImp task = new TaskImp();
        syn.openPar();
        syn.readSym(SynAnalyzer.Symbol.SS_DEFINE);                // Domain name
        syn.openPar();
        syn.readSym(SynAnalyzer.Symbol.SS_DOMAIN);
        task.domainName = syn.readId();
        syn.closePar();
        SynAnalyzer.Token token = syn.readSym(Symbol.SS_OPEN_PAR, Symbol.SS_CLOSE_PAR);
        while (token.isSym(Symbol.SS_OPEN_PAR)) {    // Domain sections
            syn.colon();
            token = syn.readSym(Symbol.SS_REQUIREMENTS, Symbol.SS_TYPES,
                    Symbol.SS_CONSTANTS, Symbol.SS_PREDICATES,
                    Symbol.SS_FUNCTIONS, Symbol.SS_MULTI_FUNCTIONS,
                    Symbol.SS_ACTION);
            switch (token.getSym()) {
                case SS_REQUIREMENTS:
                    parseRequirements(syn, task);
                    break;
                case SS_TYPES:
                    parseTypes(syn, task);
                    break;
                case SS_CONSTANTS:
                    parseObjects(syn, task, false);
                    break;
                case SS_PREDICATES:
                    parsePredicates(syn, task, false);
                    break;
                case SS_FUNCTIONS:
                    parseFunctions(syn, task, false);
                    break;
                case SS_ACTION:
                    parseAction(syn, task);
                    break;
            }
            token = syn.readSym(Symbol.SS_OPEN_PAR, Symbol.SS_CLOSE_PAR);
        }
        return task;
    }

    private String readToString(String fileName) throws IOException {
        Reader source = new java.io.FileReader(new File(System.getenv("problems"), fileName));
        StringBuilder buf = new StringBuilder();
        try {
            for (int c = source.read(); c != -1; c = source.read()) {
                buf.append((char) c);
            }
            return buf.toString();
        } catch (IOException e) {
            throw e;
        } finally {
            try {
                source.close();
            } catch (Exception e) {
            }
        }
    }

    @Override
    public AgentList parseAgentList(String agentsFile) throws ParseException, IOException {
        String content = readToString(agentsFile);
        SynAnalyzer syn = new SynAnalyzer(content);
        AgentList agList = new AgentListImp();
        SynAnalyzer.Token t;
        do {
            t = syn.readSym(SynAnalyzer.Symbol.SS_ID, SynAnalyzer.Symbol.SS_UNDEFINED);
            if (!t.undefined()) {
                String agName = t.getDesc();
                t = syn.readSym(SynAnalyzer.Symbol.SS_NUMBER);
                String ip = t.getDesc();
                agList.addAgent(agName, ip);
            }
        } while (!t.undefined());
        if (agList.isEmpty()) {
            throw new ParseException("No agents defined in the file", 1);
        }
        return agList;
    }

    private void parseRequirements(SynAnalyzer syn, TaskImp task) throws ParseException {
        SynAnalyzer.Token token;
        do {
            token = syn.readSym(Symbol.SS_COLON, Symbol.SS_CLOSE_PAR);
            if (token.isSym(Symbol.SS_COLON)) {
                String req = syn.readId().toUpperCase();
                task.addRequirement(req);
            }
        } while (!token.isSym(Symbol.SS_CLOSE_PAR));
    }

    private void parseTypes(SynAnalyzer syn, TaskImp task) throws ParseException {
        SynAnalyzer.Token token;
        ArrayList<String> typeNames;
        ArrayList<String> parentTypes;
        token = syn.readSym(Symbol.SS_ID, Symbol.SS_CLOSE_PAR);
        while (!token.isSym(Symbol.SS_CLOSE_PAR)) {                // Types list
            typeNames = new ArrayList<String>();
            while (token.isSym(Symbol.SS_ID)) {
                typeNames.add(token.getDescLower());
                token = syn.readSym(Symbol.SS_ID, Symbol.SS_DASH, Symbol.SS_CLOSE_PAR);
            }
            parentTypes = new ArrayList<String>();
            if (token.isSym(Symbol.SS_DASH)) {
                parseTypeList(syn, parentTypes, task);
                token = syn.readSym(Symbol.SS_ID, Symbol.SS_CLOSE_PAR);
            } else {
                parentTypes.add("object");
            }
            addTypes(syn, typeNames, parentTypes, task);
        }
    }

    private void parseTypeList(SynAnalyzer syn, ArrayList<String> parentTypes,
                               TaskImp task) throws ParseException {
        SynAnalyzer.Token token = syn.readSym(Symbol.SS_OPEN_PAR, Symbol.SS_ID);
        if (token.isSym(Symbol.SS_ID)) {
            TaskImp.Type type = task.new Type(token.getDescLower());
            parentTypes.add(type.name);
        } else {
            syn.readSym(Symbol.SS_EITHER);
            do {
                token = syn.readSym(Symbol.SS_ID, Symbol.SS_CLOSE_PAR);
                if (token.isSym(Symbol.SS_ID)) {
                    TaskImp.Type type = task.new Type(token.getDescLower());
                    parentTypes.add(type.name);
                }
            } while (!token.isSym(Symbol.SS_CLOSE_PAR));
        }
    }

    private TaskImp.Type addNewType(SynAnalyzer syn, String typeName, TaskImp task)
            throws ParseException {
        TaskImp.Type type = task.new Type(typeName);
        int pos = task.types.indexOf(type);
        if (pos == -1) {
            task.types.add(type);
        } else {
            if (pos > TaskImp.AGENT_TYPE) {
                syn.notifyError("Type '" + typeName + "' redefined");
            } else {
                type = task.types.get(pos);
            }
        }
        return type;
    }

    private void addTypes(SynAnalyzer syn, ArrayList<String> typeNames, ArrayList<String> parentTypes, TaskImp task) throws ParseException {
        for (String name : typeNames) {
            TaskImp.Type type = addNewType(syn, name, task);
            if (parentTypes.contains(name)) {
                type.addParentType(task.types.get(TaskImp.OBJECT_TYPE), syn);
            } else {
                for (String parent : parentTypes) {
                    int typeIndex = task.types.indexOf(task.new Type(parent));
                    if (typeIndex == -1) {
                        TaskImp.Type ptype = addNewType(syn, parent, task);
                        ptype.addParentType(task.types.get(TaskImp.OBJECT_TYPE), syn);
                        typeIndex = task.types.indexOf(ptype);
                    }
                    type.addParentType(task.types.get(typeIndex), syn);
                }
            }
        }
    }

    private void parsePredicates(SynAnalyzer syn, TaskImp task, boolean priv) throws ParseException {
        SynAnalyzer.Token token;
        do {
            token = syn.readSym(Symbol.SS_OPEN_PAR, Symbol.SS_CLOSE_PAR);
            if (token.isSym(Symbol.SS_OPEN_PAR)) {
                syn.restoreLastToken();
                TaskImp.Variable v = parsePredicate(syn, task, false, true, priv);
                if (v != null) {
                    task.predicates.add(v);
                    if (priv) {
                        privatePredicates.add(v);
                    }
                }
            }
        } while (!token.isSym(Symbol.SS_CLOSE_PAR));
    }

    private TaskImp.Variable parsePredicate(SynAnalyzer syn, TaskImp task,
                                            boolean allowDuplicates, boolean readPar, boolean priv) throws ParseException {
        if (readPar) {
            syn.openPar();
        }
        SynAnalyzer.Token token;
        if (priv) {
            token = syn.readSym(Symbol.SS_ID);
        } else {
            token = syn.readSym(Symbol.SS_ID, Symbol.SS_COLON);
        }
        if (token.isSym(Symbol.SS_COLON)) {
            readPrivateToken(syn);
            parsePredicates(syn, task, true);
            return null;
        } else {
            TaskImp.Variable v = task.new Variable(token.getDesc());
            if (!allowDuplicates && task.existVariable(v)) {
                syn.notifyError("Predicate '" + v.name + "' redefined");
            }
            ArrayList<TaskImp.Value> params = parseParameters(syn, task);
            for (TaskImp.Value p : params) {
                v.params.add(p);
            }
            if (readPar) {
                syn.closePar();
            }
            return v;
        }
    }

    private ArrayList<TaskImp.Value> parseParameters(SynAnalyzer syn, TaskImp task) throws ParseException {
        ArrayList<TaskImp.Value> res = new ArrayList<TaskImp.Value>();
        SynAnalyzer.Token token;
        ArrayList<String> paramList;
        ArrayList<String> typeList;
        do {
            token = syn.readSym(Symbol.SS_VAR, Symbol.SS_CLOSE_PAR);
            if (!token.isSym(Symbol.SS_CLOSE_PAR)) {
                String desc = token.getDescLower();
                paramList = new ArrayList<String>();
                typeList = new ArrayList<String>();
                do {
                    paramList.add(desc);
                    token = syn.readSym(Symbol.SS_VAR, Symbol.SS_DASH, Symbol.SS_CLOSE_PAR);
                    desc = token.getDescLower();
                } while (token.isSym(Symbol.SS_VAR));
                if (token.isSym(Symbol.SS_DASH)) {
                    parseTypeList(syn, typeList, task);
                } else {
                    typeList.add("object");
                }
                for (String paramName : paramList) {
                    TaskImp.Value v = task.new Value(paramName);
                    v.isVariable = true;
                    if (res.contains(v)) {
                        syn.notifyError("Parameter '" + paramName + "' redefined");
                    }
                    for (String t : typeList) {
                        v.addType(getType(t, task, syn), syn);
                    }
                    res.add(v);
                }
            }
        } while (!token.isSym(Symbol.SS_CLOSE_PAR));
        syn.restoreLastToken();
        return res;
    }

    private TaskImp.Type getType(String typeName, TaskImp task, SynAnalyzer syn) throws ParseException {
        int index = task.types.indexOf(task.new Type(typeName));
        if (index == -1) {
            syn.notifyError("Type '" + typeName + "' undefined");
        }
        return task.types.get(index);
    }

    private void readPrivateToken(SynAnalyzer syn) throws ParseException {
        String id = syn.readId();
        if (!id.equalsIgnoreCase("private")) {
            syn.notifyError("Keyword 'private' expected");
        }
    }

    private void parseAction(SynAnalyzer syn, TaskImp task) throws ParseException {
        TaskImp.Operator op = task.new Operator(syn.readId());
        if (task.operators.contains(op)) {
            syn.notifyError("Operator '" + op.name + "' redefined");
        }
        task.operators.add(op);
        syn.colon();
        boolean precRead = false, effRead = false;
        SynAnalyzer.Token token = syn.readSym(Symbol.SS_PARAMS, Symbol.SS_PREC, Symbol.SS_EFF);
        do {
            switch (token.getSym()) {
                case SS_PARAMS:
                    syn.openPar();
                    ArrayList<TaskImp.Value> params = parseParameters(syn, task);
                    for (TaskImp.Value p : params) {
                        op.params.add(p);
                    }
                    syn.closePar();
                    break;
                case SS_PREC:
                    parseOperatorCondition(syn, task, op, true);
                    precRead = true;
                    break;
                case SS_EFF:
                    parseOperatorCondition(syn, task, op, false);
                    effRead = true;
                    break;
            }
            token = syn.readSym(Symbol.SS_OPEN_PAR,
                    Symbol.SS_COLON, Symbol.SS_CLOSE_PAR);
            if (token.isSym(Symbol.SS_COLON)) {
                if (!precRead) {
                    token = syn.readSym(Symbol.SS_PREC, Symbol.SS_EFF);
                } else if (!effRead) {
                    token = syn.readSym(Symbol.SS_EFF);
                } else {
                    syn.notifyError("Unexpected colon");
                }
            }
        } while (!token.isSym(Symbol.SS_CLOSE_PAR) && !token.isSym(Symbol.SS_OPEN_PAR));
        if (token.isSym(Symbol.SS_OPEN_PAR)) {
            syn.restoreLastToken();
        }
    }

    private void parseOperatorCondition(SynAnalyzer syn, TaskImp task,
                                        TaskImp.Operator op, boolean isPrec) throws ParseException {
        TaskImp.OperatorCondition cond;
        syn.openPar();
        SynAnalyzer.Token token = syn.readSym(Symbol.SS_AND, Symbol.SS_ID,
                Symbol.SS_EQUAL);
        if (token.isSym(Symbol.SS_AND)) {            // Set of conditions
            do {
                token = syn.readSym(Symbol.SS_OPEN_PAR, Symbol.SS_CLOSE_PAR);
                if (token.isSym(Symbol.SS_OPEN_PAR)) {
                    cond = parseSingleOperatorCondition(syn, task, op, isPrec);
                    syn.closePar();
                    if (isPrec) {
                        op.prec.add(cond);
                    } else {
                        op.eff.add(cond);
                    }
                }
            } while (!token.isSym(Symbol.SS_CLOSE_PAR));
        } else {                                    // Single condition         
            syn.restoreLastToken();
            cond = parseSingleOperatorCondition(syn, task, op, isPrec);
            syn.closePar();
            if (isPrec) {
                op.prec.add(cond);
            } else {
                op.eff.add(cond);
            }
        }
    }

    private TaskImp.OperatorCondition parseSingleOperatorCondition(SynAnalyzer syn,
                                                                   TaskImp task, TaskImp.Operator op, boolean isPrec) throws ParseException {
        TaskImp.OperatorCondition cond;
        SynAnalyzer.Token token;
        if (isPrec) {
            token = syn.readSym(Symbol.SS_NOT, Symbol.SS_EQUAL, Symbol.SS_MEMBER, Symbol.SS_ID);
        } else {
            token = syn.readSym(Symbol.SS_NOT, Symbol.SS_ASSIGN, Symbol.SS_ADD, Symbol.SS_DEL,
                    Symbol.SS_INCREASE, Symbol.SS_ID);
        }
        if (token.isSym(Symbol.SS_NOT)) {            // Negation
            syn.openPar();
            cond = parseSingleOperatorCondition(syn, task, op, isPrec);
            syn.closePar();
            cond.neg = !cond.neg;
        } else if (token.isSym(Symbol.SS_INCREASE)) {
            cond = task.new OperatorCondition(TaskImp.OperatorConditionType.CT_INCREASE);
            syn.openPar();
            cond.var = parseOperatorVariable(syn, task, op);
            syn.closePar();
            TaskImp.Function function = checkFunction(cond.var, syn, task);
            if (!function.isNumeric())
                syn.notifyError("Function '" + cond.var.name + "' is not numeric");
            cond.exp = parseNumericExpression(syn, task, op, null);
        } else if (token.isSym(Symbol.SS_ID)) {        // Predicate
            syn.restoreLastToken();
            cond = task.new OperatorCondition(TaskImp.OperatorConditionType.CT_NONE);
            cond.var = parseOperatorVariable(syn, task, op);
            checkPredicate(cond.var, syn, task);
        } else {
            TaskImp.OperatorConditionType type = null;
            switch (token.getSym()) {
                case SS_EQUAL:
                    type = TaskImp.OperatorConditionType.CT_EQUAL;
                    break;
                case SS_MEMBER:
                    type = TaskImp.OperatorConditionType.CT_MEMBER;
                    break;
                case SS_ASSIGN:
                    type = TaskImp.OperatorConditionType.CT_ASSIGN;
                    break;
                case SS_ADD:
                    type = TaskImp.OperatorConditionType.CT_ADD;
                    break;
                case SS_DEL:
                    type = TaskImp.OperatorConditionType.CT_DEL;
                    break;
                default:
                    syn.notifyError("Unknown condition type");
            }
            cond = task.new OperatorCondition(type);
            syn.openPar();
            cond.var = parseOperatorVariable(syn, task, op);
            syn.closePar();
            cond.value = parseOperatorValue(syn, task, op);
            checkFunction(cond, syn, task);
        }
        return cond;
    }

    private TaskImp.Variable parseOperatorVariable(SynAnalyzer syn, TaskImp task,
                                                   TaskImp.Operator op) throws ParseException {
        TaskImp.Variable v = task.new Variable(syn.readId());
        SynAnalyzer.Token token;
        do {
            token = syn.readSym(Symbol.SS_VAR, Symbol.SS_CLOSE_PAR, Symbol.SS_ID);
            if (!token.isSym(Symbol.SS_CLOSE_PAR)) {
                syn.restoreLastToken();
                v.params.add(parseOperatorValue(syn, task, op));
            }
        } while (!token.isSym(Symbol.SS_CLOSE_PAR));
        syn.restoreLastToken();
        return v;
    }

    private TaskImp.Value parseOperatorValue(SynAnalyzer syn, TaskImp task,
                                             TaskImp.Operator op) throws ParseException {
        TaskImp.Value v;
        SynAnalyzer.Token token = syn.readSym(Symbol.SS_VAR, Symbol.SS_ID);
        if (token.isSym(Symbol.SS_VAR)) {            // Parameter
            int paramIndex = op.params.indexOf(task.new Value(token.getDescLower()));
            if (paramIndex == -1) {
                syn.notifyError("Parameter '" + token.getDesc()
                        + "' undefined in operator '" + op.name + "'");
            }
            v = op.params.get(paramIndex);
        } else {                                    // Constant
            int objIndex = task.values.indexOf(task.new Value(token.getDescLower()));
            if (objIndex == -1) {
                syn.notifyError("Constant '" + token.getDesc() + "' undefined");
            }
            v = task.values.get(objIndex);
        }
        return v;
    }

    private void checkPredicate(TaskImp.Variable var, SynAnalyzer syn, TaskImp task) throws ParseException {
        int vIndex = task.predicates.indexOf(var);
        if (vIndex == -1) {
            syn.notifyError("Predicate '" + var.name + "' undefined");
        }
        TaskImp.Variable pred = task.predicates.get(vIndex);
        if (var.params.size() != pred.params.size()) {
            syn.notifyError("Wrong number of parameters in predicate '" + var.name + "'");
        }
        for (int i = 0; i < pred.params.size(); i++) {
            TaskImp.Value predParam = pred.params.get(i),
                    varParam = var.params.get(i);
            if (!varParam.isCompatible(predParam)) {
                syn.notifyError("Invalid parameter '"
                        + varParam.name + "' in predicate '" + var.name + "'");
            }
        }
    }

    private TaskImp.Function checkFunction(TaskImp.Variable var, SynAnalyzer syn,
                                           TaskImp task) throws ParseException {
        int fIndex = task.functions.indexOf(task.new Function(var, false));
        if (fIndex == -1) {
            syn.notifyError("Function '" + var.name + "' undefined");
        }
        TaskImp.Function fnc = task.functions.get(fIndex);
        if (var.params.size() != fnc.var.params.size()) {
            syn.notifyError("Wrong number of parameters in function '" + var.name + "'");
        }
        for (int i = 0; i < var.params.size(); i++) {
            TaskImp.Value fncParam = fnc.var.params.get(i),
                    varParam = var.params.get(i);
            if (!varParam.isCompatible(fncParam)) {
                syn.notifyError("Invalid parameter '"
                        + varParam.name + "' in function '" + var.name + "'");
            }
        }
        return fnc;
    }

    private void checkFunction(TaskImp.OperatorCondition cond, SynAnalyzer syn,
                               TaskImp task) throws ParseException {
        int fIndex = task.functions.indexOf(task.new Function(cond.var, false));
        if (fIndex == -1) {
            syn.notifyError("Function '" + cond.var.name + "' undefined");
        }
        TaskImp.Function fnc = task.functions.get(fIndex);
        switch (cond.type) {
            case CT_EQUAL:
                if (fnc.multiFunction) {
                    syn.notifyError("Operator '=' not valid for multi-functions");
                }
                break;
            case CT_MEMBER:
                if (!fnc.multiFunction) {
                    syn.notifyError("Operator 'member' not valid for functions");
                }
                break;
            case CT_ASSIGN:
                if (fnc.multiFunction) {
                    syn.notifyError("Operator 'assign' not valid for multi-functions");
                }
                break;
            case CT_ADD:
                if (!fnc.multiFunction) {
                    syn.notifyError("Operator 'add' not valid for functions");
                }
                break;
            case CT_DEL:
                if (!fnc.multiFunction) {
                    syn.notifyError("Operator 'del' not valid for functions");
                }
                break;
            case CT_NONE:
                syn.notifyError("Operator expected");
        }
        if (cond.var.params.size() != fnc.var.params.size()) {
            syn.notifyError("Wrong number of parameters in function '" + cond.var.name + "'");
        }
        for (int i = 0; i < cond.var.params.size(); i++) {
            TaskImp.Value fncParam = fnc.var.params.get(i),
                    varParam = cond.var.params.get(i);
            if (!varParam.isCompatible(fncParam)) {
                syn.notifyError("Invalid parameter '"
                        + varParam.name + "' in function '" + cond.var.name + "'");
            }
        }
        if (!cond.value.isCompatible(fnc.domain)) {
            syn.notifyError("Wrong value '"
                    + cond.value.name + "' for function '" + cond.var.name + "'");
        }
    }

    @Override
    public void parseProblem(String problemFile, Task planningTask, AgentList agList, String agentName) throws ParseException, IOException {
        String content = readToString(problemFile);
        SynAnalyzer syn = new SynAnalyzer(content);
        TaskImp taskImp = (TaskImp) planningTask;
        syn.openPar();
        syn.readSym(Symbol.SS_DEFINE);                // Problem name
        syn.openPar();
        syn.readSym(Symbol.SS_PROBLEM);
        taskImp.problemName = syn.readId();
        syn.closePar();
        SynAnalyzer.Token token = syn.readSym(Symbol.SS_OPEN_PAR, Symbol.SS_CLOSE_PAR);
        while (token.isSym(Symbol.SS_OPEN_PAR)) {    // Problem sections
            syn.colon();
            token = syn.readSym(Symbol.SS_DOMAIN, Symbol.SS_OBJECTS,
                    Symbol.SS_INIT, Symbol.SS_GOAL, Symbol.SS_METRIC);
            switch (token.getSym()) {
                case SS_DOMAIN:
                    syn.readId();
                    syn.closePar();
                    break;
                case SS_OBJECTS:
                    parseObjects(syn, taskImp, false);
                    break;
                case SS_INIT:
                    parseInit(syn, taskImp);
                    break;
                case SS_GOAL:
                    parseGoal(syn, taskImp);
                    break;
                case SS_METRIC:
                    parseMetric(syn, taskImp);
                    break;
            }
            token = syn.readSym(Symbol.SS_OPEN_PAR, Symbol.SS_CLOSE_PAR);
        }
        processSharedData(syn, taskImp, agList, agentName);
    }

    private void parseObjects(SynAnalyzer syn, TaskImp task, boolean priv) throws ParseException {
        SynAnalyzer.Token token;
        ArrayList<String> objNames;
        ArrayList<String> parentTypes;
        token = priv ? syn.readSym(Symbol.SS_ID, Symbol.SS_CLOSE_PAR)
                : syn.readSym(Symbol.SS_ID, Symbol.SS_CLOSE_PAR, Symbol.SS_OPEN_PAR);
        while (!token.isSym(Symbol.SS_CLOSE_PAR)) {
            if (token.isSym(Symbol.SS_OPEN_PAR)) {
                syn.colon();
                readPrivateToken(syn);
                parseObjects(syn, task, true);
            } else {
                objNames = new ArrayList<String>();
                parentTypes = new ArrayList<String>();
                while (token.isSym(Symbol.SS_ID)) {
                    objNames.add(token.getDescLower());
                    token = syn.readSym(Symbol.SS_ID, Symbol.SS_CLOSE_PAR, Symbol.SS_DASH);
                }
                if (token.isSym(Symbol.SS_DASH)) {
                    parseTypeList(syn, parentTypes, task);
                } else {
                    parentTypes.add("object");
                }
                for (String objName : objNames) {
                    TaskImp.Value obj = addNewObject(syn, objName, task, priv);
                    for (String parent : parentTypes) {
                        TaskImp.Type type = getType(parent, task, syn);
                        obj.addType(type, syn);
                    }
                }
            }
            token = priv ? syn.readSym(Symbol.SS_ID, Symbol.SS_CLOSE_PAR)
                    : syn.readSym(Symbol.SS_ID, Symbol.SS_CLOSE_PAR, Symbol.SS_OPEN_PAR);
        }
    }

    private TaskImp.Value addNewObject(SynAnalyzer syn, String objName, TaskImp task, boolean priv)
            throws ParseException {
        TaskImp.Value v = task.new Value(objName);
        if (task.values.contains(v)) {
            syn.notifyError("Object '" + objName + "' redefined");
        }
        task.values.add(v);
        if (priv) {
            privateObjects.add(v);
        }
        return v;
    }

    private void parseInit(SynAnalyzer syn, TaskImp task) throws ParseException {
        SynAnalyzer.Token token;
        do {
            token = syn.readSym(Symbol.SS_OPEN_PAR, Symbol.SS_CLOSE_PAR);
            if (token.isSym(Symbol.SS_OPEN_PAR)) {
                syn.restoreLastToken();
                TaskImp.Assignment a = parseAssignment(syn, task, true);
                task.init.add(a);
            }
        } while (!token.isSym(Symbol.SS_CLOSE_PAR));
    }

    private TaskImp.Assignment parseAssignment(SynAnalyzer syn, TaskImp task, boolean readPar)
            throws ParseException {
        TaskImp.Assignment a;
        if (readPar) {
            syn.openPar();
        }
        SynAnalyzer.Token token = syn.readSym(Symbol.SS_EQUAL, Symbol.SS_NOT,
                Symbol.SS_PREFERENCE, Symbol.SS_ID);
        if (token.isSym(Symbol.SS_PREFERENCE)) {
            String name = syn.readId();
            a = parseAssignment(syn, task, true);
            task.addPreference(name, a, syn);
            syn.closePar();
            return null;
        }
        boolean isLiteral = !token.isSym(Symbol.SS_EQUAL);
        boolean neg = token.isSym(Symbol.SS_NOT);
        if (!neg) {
            if (isLiteral) {
                syn.restoreLastToken();
            } else {
                syn.openPar();
            }
        } else {
            syn.openPar();
            token = syn.readSym(Symbol.SS_EQUAL, Symbol.SS_ID);
            isLiteral = !token.isSym(Symbol.SS_EQUAL);
            if (isLiteral) {
                syn.restoreLastToken();
            } else {
                syn.openPar();
            }
        }
        String varName = syn.readId();
        if (isLiteral) {    // Variable
            int index = task.predicates.indexOf(task.new Variable(varName));
            if (index == -1) {
                syn.notifyError("Predicate '" + varName + "' undefined");
            }
            a = task.new Assignment(task.predicates.get(index), neg);
        } else {
            int index = task.functions.indexOf(task.new Function(task.new Variable(varName), false));
            if (index == -1) {
                syn.notifyError("Function '" + varName + "' undefined");
            }
            a = task.new Assignment(task.functions.get(index), neg);
        }
        do {                // Parameters
            token = syn.readSym(Symbol.SS_CLOSE_PAR, Symbol.SS_ID);
            if (token.isSym(Symbol.SS_ID)) {
                syn.restoreLastToken();
                TaskImp.Value param = parseAssignmentValue(syn, task);
                a.params.add(param);
            }
        } while (!token.isSym(Symbol.SS_CLOSE_PAR));
        if (isLiteral) {    // Values
            a.values.add(task.values.get(TaskImp.TRUE_VALUE));
        } else {
            if (a.fnc.multiFunction) {
                syn.readSym(Symbol.SS_OPEN_SET);
                do {
                    token = syn.readSym(Symbol.SS_CLOSE_SET, Symbol.SS_ID);
                    if (token.isSym(Symbol.SS_ID)) {
                        syn.restoreLastToken();
                        a.values.add(parseAssignmentValue(syn, task));
                    }
                } while (!token.isSym(Symbol.SS_CLOSE_SET));
            } else {
                token = syn.readSym(Symbol.SS_NUMBER, Symbol.SS_ID);
                if (token.isSym(Symbol.SS_NUMBER)) {
                    a.isNumeric = true;
                    try {
                        a.value = Double.parseDouble(token.getDesc());
                    } catch (NumberFormatException e) {
                        syn.notifyError("'" + token.getDesc() + "' is not a valid number");
                    }
                } else {
                    syn.restoreLastToken();
                    a.values.add(parseAssignmentValue(syn, task));
                }
            }
            syn.closePar();
        }
        if (neg) {
            syn.closePar();
        }
        checkAssignment(syn, task, a);
        if (!readPar) {
            syn.restoreLastToken();
        }
        return a;
    }

    private TaskImp.Value parseAssignmentValue(SynAnalyzer syn, TaskImp task) throws ParseException {
        String valueName = syn.readId().toLowerCase();
        int valueIndex = task.values.indexOf(task.new Value(valueName));
        if (valueIndex == -1) {
            syn.notifyError("Object '" + valueName + "' undefined");
        }
        return task.values.get(valueIndex);
    }

    private void checkAssignment(SynAnalyzer syn, TaskImp task, TaskImp.Assignment a) throws ParseException {
        boolean isLiteral = a.var != null;
        TaskImp.Variable var = isLiteral ? a.var : a.fnc.var;
        if (var.params.size() != a.params.size()) {
            syn.notifyError("Wrong number of parameters for literal '" + var.name + "'");
        }
        for (int i = 0; i < a.params.size(); i++) {
            TaskImp.Value predParam = var.params.get(i),
                    varParam = a.params.get(i);
            if (!varParam.isCompatible(predParam)) {
                syn.notifyError("Invalid parameter '"
                        + varParam.name + "' for literal '" + var.name + "'");
            }
        }
        if (!isLiteral) {
            for (TaskImp.Value v : a.values) {
                if (!v.isCompatible(a.fnc.domain)) {
                    syn.notifyError("Wrong value '" + v.name + "' for function '" + var.name + "'");
                }
            }
        }
    }

    private void parseGoal(SynAnalyzer syn, TaskImp task) throws ParseException {
        TaskImp.Assignment a;
        syn.openPar();
        SynAnalyzer.Token token = syn.readSym(Symbol.SS_AND, Symbol.SS_EQUAL,
                Symbol.SS_ID, Symbol.SS_PREFERENCE);
        if (token.isSym(Symbol.SS_AND)) {
            do {
                token = syn.readSym(Symbol.SS_OPEN_PAR, Symbol.SS_CLOSE_PAR);
                if (token.isSym(Symbol.SS_OPEN_PAR)) {
                    syn.restoreLastToken();
                    a = parseAssignment(syn, task, true);
                    if (a != null) {
                        task.gGoals.add(a);
                    }
                }
            } while (!token.isSym(Symbol.SS_CLOSE_PAR));
            syn.closePar();
        } else if (token.isSym(Symbol.SS_PREFERENCE)) {
            String name = syn.readId();
            a = parseAssignment(syn, task, true);
            task.addPreference(name, a, syn);
            syn.closePar();
        } else {
            syn.restoreLastToken();
            a = parseAssignment(syn, task, false);
            task.gGoals.add(a);
        }
    }

    private void processSharedData(SynAnalyzer syn, TaskImp task, AgentList agList, String agentName) throws ParseException {
        Map<TaskImp.Type, ArrayList<TaskImp.Type>> subtypesMap = generateRequiredSubtypes(syn, task);
        Map<String, TaskImp.Value> agents = generateAgentValues(agList, task, syn);
        changeObjectTypes(subtypesMap, task);
        for (TaskImp.Variable pred : task.predicates) {
            if (!privatePredicates.contains(pred)) { // Public predicate
                generateSharedData(pred, subtypesMap, task, agents, agentName);
            }
        }
    }

    private boolean usedInPublicPredicate(TaskImp.Value obj, TaskImp task) {
        boolean used = false;
        for (TaskImp.Variable pred : task.predicates) {
            if (!privatePredicates.contains(pred)) {
                for (TaskImp.Value param : pred.params)
                    if (obj.isCompatible(param)) {
                        used = true;
                        break;
                    }
                if (used) break;
            }
        }
        return used;
    }

    private Map<TaskImp.Type, ArrayList<TaskImp.Type>> generateRequiredSubtypes(SynAnalyzer syn, TaskImp task) throws ParseException {
        Map<TaskImp.Type, ArrayList<TaskImp.Type>> subtypesMap = new HashMap<>();
        for (TaskImp.Value obj : privateObjects) {
            if (usedInPublicPredicate(obj, task)) {
                if (obj.types.size() != 1)
                    throw new UnsupportedOperationException("Private objects with multiple types not supported");
                TaskImp.Type type = obj.types.get(0);
                if (!subtypesMap.containsKey(type)) {
                    TaskImp.Type pubSubtype = addNewType(syn, "pub-" + type.name, task);
                    pubSubtype.addParentType(type, syn);
                    TaskImp.Type privSubtype = addNewType(syn, "priv-" + type.name, task);
                    privSubtype.addParentType(type, syn);
                    ArrayList<TaskImp.Type> subtypes = new ArrayList<>(2);
                    subtypes.add(pubSubtype);
                    subtypes.add(privSubtype);
                    subtypesMap.put(type, subtypes);
                }
            }
        }
        return subtypesMap;
    }

    private void changeObjectTypes(Map<TaskImp.Type, ArrayList<TaskImp.Type>> subtypesMap, TaskImp task) {
        ArrayList<TaskImp.Type> subtypes;
        for (TaskImp.Value obj : task.values) {
            boolean priv = privateObjects.contains(obj);
            for (int i = 0; i < obj.types.size(); i++) {
                TaskImp.Type type = obj.types.get(i);
                subtypes = subtypesMap.get(type);
                if (subtypes != null) {
                    TaskImp.Type newType = subtypes.get(priv ? 1 : 0);
                    obj.types.set(i, newType);
                    // System.out.println(obj + " -> " + newType);
                }
            }
        }
    }

    private void generateSharedData(TaskImp.Variable pred, Map<TaskImp.Type, ArrayList<TaskImp.Type>> subtypesMap,
                                    TaskImp task, Map<String, TaskImp.Value> agList, String agentName) {
        TaskImp.Variable sharedPred = task.new Variable(pred.name);
        for (TaskImp.Value param : pred.params) {
            ArrayList<TaskImp.Type> paramTypes = getBasicTypes(param, subtypesMap, task);
            TaskImp.Value newParam = task.new Value(param.name);
            newParam.isVariable = param.isVariable;
            for (TaskImp.Type type : paramTypes) {
                ArrayList<TaskImp.Type> subtypes = subtypesMap.get(type);
                if (subtypes == null) newParam.types.add(type);
                else newParam.types.add(subtypes.get(0));
            }
            sharedPred.params.add(newParam);
        }
        TaskImp.SharedData sd = task.new SharedData(sharedPred);
        for (String agName : agList.keySet()) {
            if (!agentName.equalsIgnoreCase(agName)) {
                TaskImp.Value agent = agList.get(agName);
                sd.agents.add(agent);
            }
        }
        task.sharedData.add(sd);
    }

    private ArrayList<TaskImp.Type> getBasicTypes(TaskImp.Value param, Map<TaskImp.Type, ArrayList<TaskImp.Type>> subtypesMap, TaskImp task) {
        ArrayList<TaskImp.Type> paramTypes = new ArrayList<>();
        for (TaskImp.Type type : param.types) {
            addBasicTypes(type, paramTypes, subtypesMap, task);
        }
        return paramTypes;
    }

    private void addBasicTypes(TaskImp.Type type, ArrayList<TaskImp.Type> paramTypes, Map<TaskImp.Type, ArrayList<TaskImp.Type>> subtypesMap, TaskImp task) {
        if (!hasSubtypes(type, task)) paramTypes.add(type);
        else {
            ArrayList<TaskImp.Type> subtypes = subtypesMap.get(type);
            if (subtypes != null || areObjectsOfThisType(type, task))
                paramTypes.add(type);
            for (TaskImp.Type t : task.types) {
                if (t.parentTypes.contains(type)) {
                    if (subtypes == null ||
                            (!t.equals(subtypes.get(0)) && !t.equals(subtypes.get(1))))
                        addBasicTypes(t, paramTypes, subtypesMap, task);
                }
            }
        }
    }

    private boolean hasSubtypes(TaskImp.Type type, TaskImp task) {
        for (TaskImp.Type t : task.types) {
            if (t.parentTypes.contains(type))
                return true;
        }
        return false;
    }

    private boolean areObjectsOfThisType(TaskImp.Type type, TaskImp task) {
        for (TaskImp.Value obj : task.values) {
            if (obj.types.contains(type)) return true;
        }
        return false;
    }

    private Map<String, TaskImp.Value> generateAgentValues(AgentList agList, TaskImp task, SynAnalyzer syn) throws ParseException {
        Map<String, TaskImp.Value> agents = new HashMap<>(agList.numAgents());
        TaskImp.Type agType = getType("agent", task, syn);
        ArrayList<TaskImp.Type> agTypeList = new ArrayList<>(1);
        agTypeList.add(agType);
        for (int i = 0; i < agList.numAgents(); i++) {
            String name = agList.getName(i);
            TaskImp.Value value = task.new Value(name);
            int objIndex = task.values.indexOf(value);
            if (objIndex < 0) {
                value.types.add(agType);
                task.values.add(value);
            } else {
                value = task.values.get(objIndex);
                if (!value.isCompatible(agTypeList))
                    value.types.add(agType);
            }
            agents.put(name, value);
        }
        return agents;
    }

    private void parseFunctions(SynAnalyzer syn, TaskImp task, boolean multi) throws ParseException {
        SynAnalyzer.Token token;
        ArrayList<TaskImp.Variable> functionList;
        ArrayList<TaskImp.Type> domain;
        do {
            token = syn.readSym(Symbol.SS_OPEN_PAR, Symbol.SS_CLOSE_PAR);
            if (token.isSym(Symbol.SS_OPEN_PAR)) {
                functionList = new ArrayList<TaskImp.Variable>();
                domain = new ArrayList<TaskImp.Type>();
                do {
                    syn.restoreLastToken();
                    functionList.add(parsePredicate(syn, task, false, true, false));
                    token = syn.readSym(Symbol.SS_OPEN_PAR, Symbol.SS_DASH);
                } while (token.isSym(Symbol.SS_OPEN_PAR));
                ArrayList<String> typeNames = new ArrayList<>();
                parseTypeList(syn, typeNames, task);
                domain.clear();
                for (String type : typeNames) {
                    TaskImp.Type t = getType(type, task, syn);
                    domain.add(t);
                }
                for (TaskImp.Variable v : functionList) {
                    if (task.existVariable(v)) {
                        syn.notifyError("Function '" + v.name + "' redefined");
                    }
                    TaskImp.Function f = task.new Function(v, multi);
                    f.setDomain(domain);
                    task.functions.add(f);
                }
            }
        } while (!token.isSym(Symbol.SS_CLOSE_PAR));
    }

    private TaskImp.NumericExpressionImp parseNumericExpression(SynAnalyzer syn, TaskImp task,
                                                                TaskImp.Operator op, TaskImp.CongestionImp congestion) throws ParseException {
        SynAnalyzer.Token token = syn.readSym(Symbol.SS_OPEN_PAR, Symbol.SS_NUMBER);
        TaskImp.NumericExpressionImp exp = null;
        if (token.isSym(Symbol.SS_NUMBER)) {
            try {
                double value = Double.parseDouble(token.getDesc());
                exp = task.new NumericExpressionImp(value);
            } catch (NumberFormatException e) {
                syn.notifyError("Invalid number: " + token.getDesc());
            }
        } else {
            token = syn.readSym(Symbol.SS_PLUS, Symbol.SS_DASH, Symbol.SS_MULT,
                    Symbol.SS_DIV, Symbol.SS_NUMBER, Symbol.SS_ID);
            if (token.isSym(Symbol.SS_NUMBER)) {
                syn.restoreLastToken();
                exp = parseNumericExpression(syn, task, op, congestion);
            } else if (token.isSym(Symbol.SS_ID)) {
                if (token.getDesc().equalsIgnoreCase("usage"))
                    exp = task.new NumericExpressionImp(TaskImp.NumericExpressionType.NET_USAGE);
                else {
                    syn.restoreLastToken();
                    TaskImp.Variable v = parseOperatorVariable(syn, task, op);
                    TaskImp.Function function = checkFunction(v, syn, task);
                    if (!function.isNumeric())
                        syn.notifyError("Function '" + v.name + "' is not numeric");
                    exp = task.new NumericExpressionImp(v);
                }
            } else {
                switch (token.getSym()) {
                    case SS_PLUS:
                        exp = task.new NumericExpressionImp(TaskImp.NumericExpressionType.NET_ADD);
                        break;
                    case SS_DASH:
                        exp = task.new NumericExpressionImp(TaskImp.NumericExpressionType.NET_DEL);
                        break;
                    case SS_MULT:
                        exp = task.new NumericExpressionImp(TaskImp.NumericExpressionType.NET_PROD);
                        break;
                    case SS_DIV:
                        exp = task.new NumericExpressionImp(TaskImp.NumericExpressionType.NET_DIV);
                        break;
                }
                exp.left = parseNumericExpression(syn, task, op, congestion);
                exp.right = parseNumericExpression(syn, task, op, congestion);
            }
            syn.closePar();
        }
        return exp;
    }

    private void parseMetric(SynAnalyzer syn, TaskImp task) throws ParseException {
        syn.readSym(Symbol.SS_MINIMIZE);
        SynAnalyzer.Token token = syn.readSym(Symbol.SS_OPEN_PAR, Symbol.SS_CLOSE_PAR, Symbol.SS_TOTAL_TIME);
        if (token.isSym(Symbol.SS_TOTAL_TIME)) {
            syn.restoreLastToken();
            task.metric = parseMetricTerm(syn, task);
        } else {
            while (token.isSym(Symbol.SS_OPEN_PAR)) {
                task.metric = parseMetricTerm(syn, task);
                token = syn.readSym(Symbol.SS_OPEN_PAR, Symbol.SS_CLOSE_PAR);
            }
        }
    }

    private MetricImp parseMetricTerm(SynAnalyzer syn, TaskImp task) throws ParseException {
        MetricImp m = null;
        SynAnalyzer.Token token = syn.readSym(Symbol.SS_IS_VIOLATED, Symbol.SS_PLUS,
                Symbol.SS_MULT, Symbol.SS_TOTAL_TIME, Symbol.SS_ID);
        if (token.isSym(Symbol.SS_IS_VIOLATED)) {
            m = task.new MetricImp(syn.readId(), syn);
            syn.closePar();
        } else if (token.isSym(Symbol.SS_TOTAL_TIME)) {
            m = task.new MetricImp();
            syn.closePar();
        } else if (!token.isSym(Symbol.SS_ID)) {
            m = task.new MetricImp(token.getSym());
            token = syn.readSym(Symbol.SS_OPEN_PAR, Symbol.SS_CLOSE_PAR, Symbol.SS_NUMBER);
            while (!token.isSym(Symbol.SS_CLOSE_PAR)) {
                if (token.isSym(Symbol.SS_OPEN_PAR)) {
                    m.term.add(parseMetricTerm(syn, task));
                } else {
                    try {
                        m.term.add(task.new MetricImp(Double.parseDouble(token.getDesc())));
                    } catch (NumberFormatException e) {
                        syn.notifyError("Invalid number formatData in metric: " + token.getDesc());
                    }
                }
                token = syn.readSym(Symbol.SS_OPEN_PAR, Symbol.SS_CLOSE_PAR, Symbol.SS_NUMBER);
            }
        } else {
            m = task.new MetricImp();
            syn.closePar();
        }
        return m;
    }

    @Override
    public boolean isMAPDDL(String domainFile) throws IOException {
        String content = readToString(domainFile).toLowerCase();
        return content.contains(":factored");
    }

    @Override
    public AgentList createEmptyAgentList() {
        return new AgentListImp();
    }
}
