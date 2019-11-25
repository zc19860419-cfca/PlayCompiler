package play.compiler.script;

import cfca.org.slf4j.Logger;
import cfca.org.slf4j.LoggerFactory;
import play.compiler.script.compile.AnnotatedTree;
import play.compiler.script.compile.PlayScriptCompiler;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * PlayScript
 */
public class PlayScript {

    private static Logger LOG = LoggerFactory.getLogger(PlayScript.class);

    public static void main(String args[]) {
        //脚本
        String script = "int age = 44; for(int i = 0;i<10;i++) { age = age + 2;} int i = 8;";

        LOG.info("PlayScript#script:{}", script);
        //是否生成汇编代码
        boolean genAsm = false;

        Map params = null;

        String scriptFile = null;
        //解析参数
        try {
            params = parseParams(args);

            boolean help = params.containsKey("help") ? (Boolean) params.get("help") : false;
            if (help) {
                showHelp();
            } else {
                //从源代码读取脚本
                scriptFile = params.containsKey("scriptFile") ? (String) params.get("scriptFile") : null;
                if (scriptFile != null) {
                    script = readTextFile(scriptFile);
                }

                //打印编译过程中的信息
                boolean verbose = params.containsKey("verbose") ? (Boolean) params.get("verbose") : false;

                //打印AST
                boolean astDump = params.containsKey("ast_dump") ? (Boolean) params.get("ast_dump") : false;

                //进入REPL
                if (script == null) {
                    REPL(verbose, astDump);
                } else if (genAsm) {
                    //生成汇编代码 输出文件
                    String outputFile = params.containsKey("outputFile") ? (String) params.get("outputFile") : null;
                    generateAsm(script, outputFile);
                } else {
                    //执行脚本
                    PlayScriptCompiler compiler = new PlayScriptCompiler();
                    AnnotatedTree at = compiler.compile(script, verbose, astDump);

                    if (!at.hasCompilationError()) {
                        Object result = compiler.Execute(at);
                        LOG.debug("PlayScript#compiler result:{} ", result);
                    }
                }
            }

        } catch (IOException e) {
            LOG.error("PlayScript#Failed:unable to read from {} ", scriptFile, e);
        } catch (Exception e) {
            LOG.error("PlayScript#Failed", e);
        }
    }

    /**
     * 生成ASM
     *
     * @param script     脚本
     * @param outputFile 输出的文件名
     */
    private static void generateAsm(String script, String outputFile) {

    }

    /**
     * REPL:简单交互式的编程环境
     */
    private static void REPL(boolean verbose, boolean astDump) {

    }

    /**
     * 读文本文件
     *
     * @param pathName
     * @return
     * @throws IOException
     */
    private static String readTextFile(String pathName) {
        return null;
    }

    /**
     * 打印帮助信息
     */
    private static void showHelp() {
        StringBuilder builder = new StringBuilder(1024);
        builder.append("usage: java play.PlayScript [-h | --help | -o outputfile | -S | -v | -ast-dump] [scriptfile]");
        builder.append("\t-h or --help : print this help information");
        builder.append("\t-v verbose mode : dump AST and symbols");
        builder.append("\t-ast-dump : dump AST in lisp style");
        builder.append("\t-o outputfile : file pathname used to save generated code, eg. assembly code");
        builder.append("\t-S : compile to assembly code");
        builder.append("\tscriptfile : file contains playscript code");

        builder.append("\nexamples:");
        builder.append("\tjava play.PlayScript");
        builder.append("\t>>interactive REPL mode");
        builder.append("\n");

        builder.append("\tjava play.PlayScript -v");
        builder.append("\t>>enter REPL with verbose mode, dump ast and symbols");
        builder.append("\n");

        builder.append("\tjava play.PlayScript scratch.play");
        builder.append("\t>>compile and execute scratch.play");
        builder.append("\n");

        builder.append("\tjava play.PlayScript -v scratch.play");
        builder.append("\t>>compile and execute scratch.play in verbose mode, dump ast and symbols");
        builder.append("\n");
        LOG.info(builder.toString());
    }

    /**
     * 解析参数
     *
     * @param args
     * @return
     */
    private static Map parseParams(String[] args) throws Exception {

        Map<String, Object> params = new HashMap<>();

        for (int i = 0; i < args.length; i++) {
            /**
             * 输出汇编代码
             */
            if (args[i].equals("-S")) {
                params.put("genAsm", true);
            }

            /**
             * 显示作用域和符号
             */
            else if (args[i].equals("-h") || args[i].equals("--help")) {
                params.put("help", true);
            }

            /**
             * 显示作用域和符号
             */
            else if (args[i].equals("-v")) {
                params.put("verbose", true);
            }

            /**
             * 显示作用域和符号
             */
            else if (args[i].equals("-ast-dump")) {
                params.put("ast_dump", true);
            }

            /**
             * 输出文件
             */
            else if (args[i].equals("-o")) {
                if (i + 1 < args.length) {
                    params.put("outputFile", args[++i]);
                } else {
                    throw new Exception("Expecting a filename after -o");
                }
            }

            /**
             * 不认识的参数
             */
            else if (args[i].startsWith("-")) {
                throw new Exception("Unknow parameter : " + args[i]);
            }

            /**
             * 脚本文件
             */
            else {
                params.put("scriptFile", args[i]);
            }
        }

        return params;
    }
}
