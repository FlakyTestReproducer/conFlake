import soot.*;
import soot.javaToJimple.LocalGenerator;
import soot.jimple.*;
import soot.jimple.infoflow.android.SetupApplication;
import soot.jimple.internal.JIdentityStmt;
import soot.options.Options;
import soot.util.Chain;
import utils.LoadProperties;

import java.util.*;

public class SootInstrument {
    private static String sdkPath;
    private SetupApplication app;
    private final String INIT_METHOD = "void <init>()";
    private long sleeptime;

    public void init(String apkPath){
        app = new SetupApplication(sdkPath,apkPath);
    }

    public void initSoot(String apkPath){
        sdkPath = LoadProperties.get("SDKPATH").replace("\\","");
        sleeptime = Long.valueOf(LoadProperties.get("DELAY"));
        Options.v().set_process_multiple_dex(true);
        Options.v().set_whole_program(true);
        Options.v().set_no_writeout_body_releasing(true);

        Options.v().set_allow_phantom_refs(true);
        Options.v().set_prepend_classpath(true);
        Options.v().set_output_format(Options.output_format_dex);
        Options.v().set_android_jars(sdkPath);
        Options.v().set_src_prec(Options.src_prec_apk);
        Options.v().set_process_dir(Collections.singletonList(apkPath));
        Options.v().set_force_overwrite(true);
        Scene.v().loadNecessaryClasses();
    }

    public void instrument(){
//        PackManager.v().getPack("jtp").add(
//                new Transform("jtp.MyTransform", new MyTransform2()));//add our own BodyTransformer
        //PackManager.v().getPack("wjtp").add(new Transform("wjtp.patcher",new IMTransform()));

        PackManager.v().getPack("jtp").add(new Transform("jtp.patcher",new DelayTransform2()));
        PackManager.v().runPacks();
        PackManager.v().writeOutput();
//        String mainArgs =
//                "-android-api-version 23"; // "-process-dir " + apk.toFile().getAbsolutePath();
//        soot.Main.main(mainArgs.split("\\s"));
    }


    class MyTransform extends BodyTransformer {

        @Override
        protected void internalTransform(Body body, String s, Map<String, String> map) {
            Iterator<Unit> unitsIterator = body.getUnits().snapshotIterator();
            while(unitsIterator.hasNext()){
                Stmt stmt = (Stmt) unitsIterator.next();
                if (stmt.containsInvokeExpr()) {
                    String declaringClass = stmt.getInvokeExpr().getMethod().getDeclaringClass().getName();
                    String methodName = stmt.getInvokeExpr().getMethod().getName();
                    if (methodName.equals("onCreate")) {
                        List<Unit> toastUnits = makeToast(body, "insert info");
                        body.getUnits().insertAfter(toastUnits, stmt);
                        break;
                    }
                }
            }
        }
    }

    class MyTransform2 extends BodyTransformer{

        @Override
        protected void internalTransform(Body b, String s, Map<String, String> map) {
            SootMethod bMethod = b.getMethod();
            SootClass bodyClass = bMethod.getDeclaringClass();
            SootClass sc = Scene.v().getSootClassUnsafe("com.fd.se.statelossdemo.MainActivity");
            if(bMethod.getName().equals("onCreate") && sc.getName().equals(bodyClass.getName())){
                SootField isSubmit = new SootField("isSubmit",BooleanType.v(),Modifier.PRIVATE);
                sc.addField(isSubmit);
                if(!sc.declaresMethod("void onPause()")){
                    SootMethod method = new SootMethod("onPause", Arrays.asList(), VoidType.v(), Modifier.PROTECTED);
                    sc.addMethod(method);
                    JimpleBody body = Jimple.v().newBody(method);
                    method.setActiveBody(body);



                    body.insertIdentityStmts();
                    Chain units = body.getUnits();
                    ReturnVoidStmt returnStmt = Jimple.v().newReturnVoidStmt();
                    units.addLast(returnStmt);


                    Local thisLocal = body.getThisLocal();
                    SootMethod parentOnPauseMethod = getSuperOnPause(sc);
                    SpecialInvokeExpr superInvoke =
                            Jimple.v().newSpecialInvokeExpr(thisLocal, parentOnPauseMethod.makeRef());
                    InvokeStmt superInvokeStmt = Jimple.v().newInvokeStmt(superInvoke);
                    units.insertBefore(superInvokeStmt,returnStmt);

                    SootClass logClass = Scene.v().getSootClass("android.util.Log");//获取android.util.Log类
                    SootMethod sootMethod = logClass.getMethod("int i(java.lang.String,java.lang.String)");
                    StaticInvokeExpr staticInvokeExpr = Jimple.v().newStaticInvokeExpr(sootMethod.makeRef(), StringConstant.v("test"), StringConstant.v("new method onPause"));
                    InvokeStmt invokeStmt = Jimple.v().newInvokeStmt(staticInvokeExpr);


                    units.insertAfter(invokeStmt,superInvokeStmt);


                    NopStmt newNopStmt = Jimple.v().newNopStmt();
                    Local boolLocal = generateNewLocal(body,BooleanType.v());
//                    Local boolLocal = Jimple.v().newLocal("isSubmit",BooleanType.v());
//                    body.getLocals().add(boolLocal);
                    //AssignStmt assignStmt = Jimple.v().newAssignStmt(boolLocal, IntConstant.v(0));
                    FieldRef fieldRef = Jimple.v().newInstanceFieldRef(thisLocal,isSubmit.makeRef());

                    AssignStmt assignStmt = Jimple.v().newAssignStmt(boolLocal,fieldRef);
                    AssignStmt fieldAssign = Jimple.v().newAssignStmt(fieldRef, IntConstant.v(1));
                    units.insertAfter(assignStmt,invokeStmt);
                    units.insertAfter(fieldAssign,invokeStmt);
                    EqExpr expr = Jimple.v().newEqExpr(boolLocal,IntConstant.v(1));

                    StaticInvokeExpr staticInvokeExpr1 = Jimple.v().newStaticInvokeExpr(sootMethod.makeRef(),StringConstant.v("test"),StringConstant.v("if target"));
                    InvokeStmt invokeStmt1 = Jimple.v().newInvokeStmt(staticInvokeExpr1);

                    StaticInvokeExpr staticInvokeExpr2 = Jimple.v().newStaticInvokeExpr(sootMethod.makeRef(),StringConstant.v("test"),StringConstant.v("else target"));
                    InvokeStmt invokeStmt2 = Jimple.v().newInvokeStmt(staticInvokeExpr2);
                    IfStmt ifStmt = Jimple.v().newIfStmt(expr,invokeStmt1);

                    units.insertAfter(ifStmt,assignStmt);

                    units.insertBefore(invokeStmt2,returnStmt);
                    GotoStmt gotoElseNop = Jimple.v().newGotoStmt(newNopStmt);
                    units.insertBefore(gotoElseNop, returnStmt);

                    units.insertBefore(invokeStmt1,returnStmt);
                    GotoStmt gotoNop = Jimple.v().newGotoStmt(newNopStmt);
                    units.insertBefore(gotoNop, returnStmt);

                    units.insertBefore(newNopStmt, returnStmt);







                }
                System.out.println("insert method successfully");
            }

        }
    }


    class IMTransform extends SceneTransformer{

        @Override
        protected void internalTransform(String s, Map<String, String> map) {
            SootClass sc = Scene.v().getSootClass("com.fd.se.statelossdemo.MainActivity");
            SootField isSubmit = new SootField("isSubmit",BooleanType.v(),Modifier.PRIVATE);
            sc.addField(isSubmit);
            if(!sc.declaresMethod("void onPause()")){
                SootMethod method = new SootMethod("onPause", Arrays.asList(), VoidType.v(), Modifier.PROTECTED);
                sc.addMethod(method);
                JimpleBody body = Jimple.v().newBody(method);
                method.setActiveBody(body);



                body.insertIdentityStmts();
                Chain units = body.getUnits();
                ReturnVoidStmt returnStmt = Jimple.v().newReturnVoidStmt();
                units.addLast(returnStmt);


                Local thisLocal = body.getThisLocal();
                SootMethod parentOnPauseMethod = getSuperOnPause(sc);
                SpecialInvokeExpr superInvoke =
                        Jimple.v().newSpecialInvokeExpr(thisLocal, parentOnPauseMethod.makeRef());
                InvokeStmt superInvokeStmt = Jimple.v().newInvokeStmt(superInvoke);
                units.insertBefore(superInvokeStmt,returnStmt);

                SootClass logClass = Scene.v().getSootClass("android.util.Log");//获取android.util.Log类
                SootMethod sootMethod = logClass.getMethod("int i(java.lang.String,java.lang.String)");
                StaticInvokeExpr staticInvokeExpr = Jimple.v().newStaticInvokeExpr(sootMethod.makeRef(), StringConstant.v("test"), StringConstant.v("new method onPause"));
                InvokeStmt invokeStmt = Jimple.v().newInvokeStmt(staticInvokeExpr);


                units.insertAfter(invokeStmt,superInvokeStmt);


                NopStmt newNopStmt = Jimple.v().newNopStmt();
                Local boolLocal = generateNewLocal(body,BooleanType.v());
//                    Local boolLocal = Jimple.v().newLocal("isSubmit",BooleanType.v());
//                    body.getLocals().add(boolLocal);
                //AssignStmt assignStmt = Jimple.v().newAssignStmt(boolLocal, IntConstant.v(0));
                FieldRef fieldRef = Jimple.v().newInstanceFieldRef(thisLocal,isSubmit.makeRef());

                AssignStmt assignStmt = Jimple.v().newAssignStmt(boolLocal,fieldRef);
                AssignStmt fieldAssign = Jimple.v().newAssignStmt(fieldRef, IntConstant.v(1));
                units.insertAfter(assignStmt,invokeStmt);
                units.insertAfter(fieldAssign,invokeStmt);
                EqExpr expr = Jimple.v().newEqExpr(boolLocal,IntConstant.v(1));

                StaticInvokeExpr staticInvokeExpr1 = Jimple.v().newStaticInvokeExpr(sootMethod.makeRef(),StringConstant.v("test"),StringConstant.v("if target"));
                InvokeStmt invokeStmt1 = Jimple.v().newInvokeStmt(staticInvokeExpr1);

                StaticInvokeExpr staticInvokeExpr2 = Jimple.v().newStaticInvokeExpr(sootMethod.makeRef(),StringConstant.v("test"),StringConstant.v("else target"));
                InvokeStmt invokeStmt2 = Jimple.v().newInvokeStmt(staticInvokeExpr2);
                IfStmt ifStmt = Jimple.v().newIfStmt(expr,invokeStmt1);

                units.insertAfter(ifStmt,assignStmt);

                units.insertBefore(invokeStmt2,returnStmt);
                GotoStmt gotoElseNop = Jimple.v().newGotoStmt(newNopStmt);
                units.insertBefore(gotoElseNop, returnStmt);

                units.insertBefore(invokeStmt1,returnStmt);
                GotoStmt gotoNop = Jimple.v().newGotoStmt(newNopStmt);
                units.insertBefore(gotoNop, returnStmt);

                units.insertBefore(newNopStmt, returnStmt);
            }
            System.out.println("insert method successfully");
        }
    }

    class DelayTransform extends SceneTransformer{

        @Override
        protected void internalTransform(String s, Map<String, String> map) {
            SootClass sc = Scene.v().getSootClassUnsafe("com.fd.se.statelossdemo.MainActivity");
            SootMethod method = sc.getMethod("void onCreate(android.os.Bundle)");
            Body b = method.getActiveBody();
            method.setActiveBody(b);
            Chain<Unit> units = b.getUnits();
            Unit first = units.getFirst();
            Unit last = units.getLast();
            Unit delay = threadSleep();
            units.insertBefore(delay,first);
            SootClass exception = Scene.v().getSootClass("java.lang.InterruptedException");
            Jimple.v().newTrap(exception,delay,first,last);
        }
    }

    class DelayTransform2 extends BodyTransformer{

        @Override
        protected void internalTransform(Body body, String s, Map<String, String> map) {
            SootMethod bMethod = body.getMethod();
            SootClass bodyClass = bMethod.getDeclaringClass();
//            SootClass sc = Scene.v().getSootClassUnsafe("com.fd.se.statelossdemo.MainActivity");
//            if(bMethod.getName().equals("onCreate") && sc.getName().equals(bodyClass.getName())){
//                Chain<Unit> units = body.getUnits();
//                Unit insertion = getInsertion(units);
//                Unit delay = threadSleep();
//                insertDelay(body,units,delay,insertion);
//                return;
//            }
//            if(bMethod.getSubSignature().equals("void run()") && bodyClass.getName().startsWith("com.evgenii.jsevaluator.JsEvaluator$")){
//                Chain<Unit> units = body.getUnits();
//                Unit insertion = getInsertion(units);
//                //Unit delay = threadSleep();
//                //insertDelay(body,units,delay,insertion);
//                Unit delay = delayMethodCall();
//                insertDelayMul(units,delay,insertion);
//                return;
//            }

            String subsignature = bMethod.getSubSignature();
//            if(subsignature.equals("java.lang.Void call()") || subsignature.equals("void run()")){
//                Chain<Unit> units = body.getUnits();
//                Unit insertion = getInsertion(units);
//                Unit delay = delayMethodCall();
//                insertDelayMul(units,delay,insertion);
//                return;
//            }
            if(subsignature.endsWith("call()") || subsignature.equals("void run()")){
                Chain<Unit> units = body.getUnits();
                Unit insertion = getInsertion(units);
                Unit delay = delayMethodCall();
                insertDelayMul(units,delay,insertion);
                return;
            }

//            if(subsignature.equals("void run()") && bodyClass.getName().startsWith("com.evgenii.jsevaluator.JsEvaluator$")){
//                Chain<Unit> units = body.getUnits();
//                Unit insertion = getInsertion(units);
//                Unit delay = delayMethodCall();
//                insertDelayMul(units,delay,insertion);
//                return;
//            }
//            if(subsignature.equals("java.lang.Void call()") && bodyClass.getName().startsWith("androidx.test.espresso.ViewInteraction$")){
//                Local viewaction = null;
//                Chain<Local> locals = body.getLocals();
//                for(Local local : locals){
//                    String type = local.getType().toString();
//                    if(type.equals("androidx.test.espresso.ViewInteraction$SingleExecutionViewAction")){
//                        viewaction = local;
//                        Chain<Unit> units = body.getUnits();
//
//                        String classString = LoadProperties.get("CLASS");
//                        SootClass sootClass = Scene.v().getSootClass(classString);
//                        SootMethod sootMethod = sootClass.getMethod("void insertDelayforTask(androidx.test.espresso.ViewAction)");
//                        StaticInvokeExpr staticInvokeExpr = Jimple.v().newStaticInvokeExpr(sootMethod.makeRef(),viewaction);
//                        InvokeStmt invokeStmt = Jimple.v().newInvokeStmt(staticInvokeExpr);
//                        units.insertAfter(invokeStmt,getInsertion(units,viewaction));
//                        break;
//                    }
//                }
//                return;
//            }

//            if(subsignature.endsWith("call()") && bodyClass.getName().startsWith("androidx.test.espresso.base.UiControllerImpl$2")){
//                Chain<Unit> units = body.getUnits();
//                Unit insertion = getInsertion(units);
//                Unit delay = delayMethodCall();
//                insertDelayMul(units,delay,insertion);
//                return;
//            }
        }
    }

    /**
     * insert delay before Unit last
     * @param body body of method
     * @param units units of method
     * @param delay the delay unit
     * @param last the insertion point
     */
    private void insertDelay(Body body, Chain<Unit> units, Unit delay, Unit last){
        units.insertBefore(delay,last);
        NopStmt trynop = Jimple.v().newNopStmt();
        units.insertBefore(trynop,delay);

        SootClass randomClass = Scene.v().getSootClass("java.util.Random");
        Local random = generateNewLocal(body,RefType.v(randomClass));

        NewExpr randomNew = Jimple.v().newNewExpr(randomClass.getType());
        AssignStmt randomNewStmt = Jimple.v().newAssignStmt(random,randomNew);
        SootMethod initMethod = randomClass.getMethodUnsafe(INIT_METHOD);
        SpecialInvokeExpr initInvokeExpr = Jimple.v().newSpecialInvokeExpr(random, initMethod.makeRef());
        InvokeStmt initStmt = Jimple.v().newInvokeStmt(initInvokeExpr);
        units.insertBefore(randomNewStmt,trynop);
        units.insertAfter(initStmt,randomNewStmt);
        SootMethod nextIntMethod = randomClass.getMethodUnsafe("int nextInt(int)");
        VirtualInvokeExpr nextIntExpr = Jimple.v().newVirtualInvokeExpr(random, nextIntMethod.makeRef(),IntConstant.v(4000));
        Local immediate = generateNewLocal(body,IntType.v());
        AssignStmt immediateAssign = Jimple.v().newAssignStmt(immediate,nextIntExpr);
        units.insertAfter(immediateAssign,initStmt);
        AddExpr addExpr = Jimple.v().newAddExpr(immediate, IntConstant.v(1000));
        Local randomNum = generateNewLocal(body, RefType.v("int"));
        AssignStmt randomAssign = Jimple.v().newAssignStmt(randomNum,addExpr);
        units.insertAfter(randomAssign,immediateAssign);
        ((InvokeStmt)delay).getInvokeExpr().setArg(0,randomNum);


        NopStmt aftcatch = Jimple.v().newNopStmt();
        units.insertBefore(aftcatch,last);
        NopStmt midnop = Jimple.v().newNopStmt();
        units.insertAfter(midnop,delay);
        GotoStmt gotoStmt = Jimple.v().newGotoStmt(aftcatch);
        units.insertAfter(gotoStmt,midnop);
        NopStmt catchnop = Jimple.v().newNopStmt();
        units.insertAfter(catchnop,gotoStmt);

        SootClass logClass = Scene.v().getSootClass("android.util.Log");
        SootMethod sootMethod = logClass.getMethod("int e(java.lang.String,java.lang.String)");
        StaticInvokeExpr staticInvokeExpr = Jimple.v().newStaticInvokeExpr(sootMethod.makeRef(), StringConstant.v("delay info"), StringConstant.v("delay failed"));
        InvokeStmt invokeStmt = Jimple.v().newInvokeStmt(staticInvokeExpr);
        units.insertAfter(invokeStmt,catchnop);

        SootClass exception = Scene.v().getSootClass("java.lang.InterruptedException");
        Trap t = Jimple.v().newTrap(exception,trynop,midnop,catchnop);
        body.getTraps().addFirst(t);
    }

    private void insertDelayMul(Chain<Unit> units, Unit delay, Unit last){
        units.insertBefore(delay,last);
    }

    /**
     * get the insertion of method body
     * @param units units of body
     * @return
     */
    private Unit getInsertion(Chain<Unit> units){
        Unit unit = units.getFirst();
        while(unit != null){
            if(unit instanceof IdentityStmt){
//                IdentityStmt identity = (IdentityStmt) unit;
//                Value right = identity.getRightOp();
//                if(right instanceof IdentityRef){
//                    unit = units.getSuccOf(unit);
//                }else{
//                    break;
//                }
                unit = units.getSuccOf(unit);
            }else{
                break;
            }
        }
        if(unit != null){
            return unit;
        }
        return units.getSuccOf(units.getFirst());
    }

    private Unit getInsertion(Chain<Unit> units, Local local){
        Unit unit = units.getFirst();
        while(unit != null){
            if(unit instanceof AssignStmt){
                AssignStmt assign = (AssignStmt) unit;
                Value left = assign.getLeftOp();
                if(left.equivTo(local)){
                    return assign;
                }
            }
            unit = units.getSuccOf(unit);
        }
        return units.getSuccOf(units.getFirst());
    }


    private List<Unit> makeToast(Body body, String toast) {
        List<Unit> unitsList = new ArrayList<Unit>();
        //插入语句Log.i("test",toast);
        SootClass logClass = Scene.v().getSootClass("android.util.Log");//获取android.util.Log类
        SootMethod sootMethod = logClass.getMethod("int i(java.lang.String,java.lang.String)");
        StaticInvokeExpr staticInvokeExpr = Jimple.v().newStaticInvokeExpr(sootMethod.makeRef(), StringConstant.v("test"), StringConstant.v(toast));
        InvokeStmt invokeStmt = Jimple.v().newInvokeStmt(staticInvokeExpr);
        unitsList.add(invokeStmt);
        return unitsList;
    }

    private Unit threadSleep(){
        SootClass threadClass = Scene.v().getSootClass("java.lang.Thread");
        SootMethod sootMethod = threadClass.getMethod("void sleep(long)");
        StaticInvokeExpr staticInvokeExpr = Jimple.v().newStaticInvokeExpr(sootMethod.makeRef(), LongConstant.v(sleeptime));
        InvokeStmt invokeStmt = Jimple.v().newInvokeStmt(staticInvokeExpr);
        return invokeStmt;
    }

    private Unit delayMethodCall(){
        String classString = LoadProperties.get("CLASS");
        SootClass sootClass = Scene.v().getSootClass(classString);
        SootMethod sootMethod = sootClass.getMethod("void insertDelay()");
        StaticInvokeExpr staticInvokeExpr = Jimple.v().newStaticInvokeExpr(sootMethod.makeRef());
        InvokeStmt invokeStmt = Jimple.v().newInvokeStmt(staticInvokeExpr);
        return invokeStmt;
    }

    private Unit delayMethodCallforTask(){
        String classString = LoadProperties.get("CLASS");
        SootClass sootClass = Scene.v().getSootClass(classString);
        SootMethod sootMethod = sootClass.getMethod("void insertDelayforTask()");
        StaticInvokeExpr staticInvokeExpr = Jimple.v().newStaticInvokeExpr(sootMethod.makeRef());
        InvokeStmt invokeStmt = Jimple.v().newInvokeStmt(staticInvokeExpr);
        return invokeStmt;
    }

    private static SootMethod getSuperOnPause(SootClass activity) {
        SootMethod parentOnPause = null;
        SootClass superClass = activity.getSuperclass();
        while (parentOnPause == null && !superClass.getName().equals("java.lang.Object")) {
            parentOnPause =
                    superClass.getMethodUnsafe("void onPause()");
            superClass = superClass.getSuperclass();
        }
        return parentOnPause;
    }

    private Local generateNewLocal(Body body, Type type) {
        LocalGenerator lg = new LocalGenerator(body);
        return lg.generateLocal(type);
    }
}
