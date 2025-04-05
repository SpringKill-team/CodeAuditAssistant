package org.skgroup.codeauditassistant.utils

import org.skgroup.codeauditassistant.enums.SubVulnerabilityDefinition
import org.skgroup.codeauditassistant.enums.SubVulnerabilityType

/**
 * Sink list 是一个存储所有 Sink 的类，其中包含了所有的 Sink 的定义
 *
 * @author springkill
 * @version 1.0
 */
object SinkList {

    private val nettyResponseSplittingDefinition = SubVulnerabilityDefinition(
        subType = SubVulnerabilityType.NETTY_RESPONSE_SPLITTING,
        methodSinks = mapOf(
            "io.netty.handler.codec.http.HttpRequestEncoder" to setOf("encode"),
            "io.netty.handler.codec.http.HttpResponseDecoder" to setOf("decode"),
            "io.netty.handler.codec.http.HttpObjectAggregator" to setOf("decode"),
        ),
        constructorSinks = setOf(
            "io.netty.handler.codec.http.DefaultHttpHeaders",
            "io.netty.handler.codec.http.DefaultHttpResponse"
        ),
        isCall = true
    )

    private val systemDosDefinition = SubVulnerabilityDefinition(
        subType = SubVulnerabilityType.SYSTEM_DOS,
        methodSinks = mapOf(
            "java.lang.System" to setOf("exit"),
            "java.lang.Shutdown" to setOf("exit"),
            "java.lang.Runtime" to setOf("exit")
        ),
        constructorSinks = emptySet(),
        isCall = true
    )

    private val pattenDosDefinition = SubVulnerabilityDefinition(
        subType = SubVulnerabilityType.PATTERN_DOS,
        methodSinks = mapOf(
            "java.util.regex.Pattern" to setOf("compile", "matches")
        ),
        constructorSinks = emptySet(),
        isCall = true
    )

    private val readFileDefinition = SubVulnerabilityDefinition(
        subType = SubVulnerabilityType.READ_FILE,
        methodSinks = mapOf(
            "java.lang.Class" to setOf("getResourceAsStream"),
            "org.apache.commons.io.FileUtils" to setOf(
                "readFileToByteArray",
                "readFileToString",
                "readLines"
            ),
            "java.nio.file.Files" to setOf(
                "readAllBytes",
                "readAllLines"
            ),
            "java.io.BufferedReader" to setOf("readLine")
        ),
        constructorSinks = emptySet(),
        isCall = true
    )

    private val commonsIODefinition = SubVulnerabilityDefinition(
        subType = SubVulnerabilityType.COMMONS_IO,
        methodSinks = mapOf(
            "org.apache.commons.io.file.PathFilter" to setOf("accept"),
            "org.apache.commons.io.file.PathUtils" to setOf("copyFile", "copyFileToDirectory"),
            "org.apache.commons.io.filefilter.FileFilterUtils" to setOf("filter", "filterList", "filterSet"),
            "org.apache.commons.io.output.DeferredFileOutputStream" to setOf("writeTo"),
            "org.apache.commons.io.output.FileWriterWithEncoding" to setOf("write"),
            "org.apache.commons.io.output.LockableFileWriter" to setOf("write"),
            "org.apache.commons.io.output.XmlStreamWriter" to setOf("write"),
            "org.apache.commons.io.FileUtils" to setOf(
                "copyDirectory", "copyDirectoryToDirectory", "copyFile", "copyFileToDirectory",
                "copyInputStreamToFile", "copyToDirectory", "copyToFile", "moveDirectory",
                "moveDirectoryToDirectory", "moveFile", "moveFileToDirectory", "touch", "write",
                "writeByteArrayToFile", "writeLines", "writeStringToFile"
            ),
            "org.apache.commons.io.IOUtils" to setOf("copy"),
            "org.apache.commons.io.RandomAccessFileMode" to setOf("create"),
            "org.apache.commons.fileupload" to setOf("write")
        ),
        constructorSinks = emptySet(),
        isCall = true
    )

    private val ioFiles = SubVulnerabilityDefinition(
        subType = SubVulnerabilityType.IO_FILES,
        methodSinks = mapOf(
            "java.io.FileOutputStream" to setOf("write"),
            "java.nio.file.Files" to setOf(
                "write", "writeString", "copy", "move", "createDirectory", "createFile",
                "createLink", "createSymbolicLink", "createTempDirectory", "createTempFile",
                "createDirectories"
            ),
            "org.springframework.web.multipart.MultipartFile" to setOf("transferTo"),
            "org.apache.tomcat.http.fileupload.FileItem" to setOf("FileItem"),
            "javax.servlet.http.Part" to setOf("write"),
//            "java.io.PrintWriter" to setOf("print", "write", "format", "printf", "println"),
            "java.io.RandomAccessFile" to setOf("write", "writeBytes", "writeChars", "writeUTF"),
            "org.springframework.util.FileCopyUtils" to setOf("copy"),
            "java.io.FileWriter" to setOf("append", "write"),
//            "java.io.Writer" to setOf("append", "write"),
//            "java.io.BufferedWriter" to setOf("write"),
//            "java.io.OutputStream" to setOf("write"),
//            "java.io.ByteArrayOutputStream" to setOf("writeTo"),
//            "java.io.BufferedOutputStream" to setOf("write"),
            "java.io.DataOutputStream" to setOf("writeByte", "writeBytes", "writeChars", "writeUTF"),
//            "java.io.OutputStreamWriter" to setOf("write", "append"),
//            "java.io.ObjectOutputStream" to setOf("writeObject"),
//    "java.io.PrintStream" to setOf("append", "format", "print", "printf", "println", "write", "writeBytes")
        ),
        constructorSinks = emptySet(),
        isCall = true
    )

    private val jdbcAttackDefinition = SubVulnerabilityDefinition(
        subType = SubVulnerabilityType.JDBC_ATTACK,
        methodSinks = mapOf(
            "javax.sql.DataSource" to setOf("getConnection"),
            "java.sql.Driver" to setOf("connect"),
            "java.sql.DriverManager" to setOf("getConnection"),
            "org.springframework.jdbc.DataSourceBuilder" to setOf("url"),
            "org.jdbi.v3.core.Jdbi" to setOf("create", "open"),
            "com.zaxxer.hikari.HikariConfig" to setOf("setJdbcUrl"),
            "org.springframework.jdbc.datasource.AbstractDriverBasedDataSource" to setOf("setUrl"),
            "org.apache.commons.dbcp2.BasicDataSource" to setOf("setUrl"),
            "com.mchange.v2.c3p0.ComboPooledDataSource" to setOf("setJdbcUrl"),
            "org.h2.jdbcx.JdbcDataSource" to setOf("setUrl"),  // H2 数据库
            "org.h2.Driver" to setOf("connect")                // H2 驱动
        ),
        constructorSinks = setOf(
            "org.springframework.jdbc.datasource.DriverManagerDataSource",
            "com.zaxxer.hikari.HikariConfig",
            "org.apache.commons.dbcp2.BasicDataSource",  // Apache DBCP
            "com.mchange.v2.c3p0.ComboPooledDataSource", // C3P0
            "org.h2.jdbcx.JdbcDataSource"               // H2 数据库
        ),
        isCall = true
    )

    private val jndiInjectionDefinition = SubVulnerabilityDefinition(
        subType = SubVulnerabilityType.JNDI_INJECTION,
        methodSinks = mapOf(
            "java.rmi.registry.Registry" to setOf("lookup"),
            "javax.naming.Context" to setOf("lookup", "list", "listBindings", "lookupLink", "rename"),
            "javax.naming.InitialContext" to setOf("doLookup", "lookup", "rename", "list", "listBindings"),
            "javax.naming.directory.InitialDirContext" to setOf("lookup", "rename", "list", "listBindings"),
            "javax.naming.ldap.InitialLdapContext" to setOf("lookup", "rename", "list", "listBindings", "lookupLink"),
            "javax.management.remote.JMXConnector" to setOf("connect"),
            "javax.management.remote.JMXConnectorFactory" to setOf("connect"),
            "org.springframework.ldap.LdapOperations" to setOf(
                "findByDn", "list", "listBindings", "lookup", "lookupContext", "rename", "search"
            ),
            "org.springframework.ldap.core.LdapOperation" to setOf(
                "findByDn", "list", "listBindings", "lookup", "lookupContext", "rename", "search", "searchForObject"
            ),
            "org.springframework.ldap.core.LdapTemplate" to setOf(
                "lookup", "lookupContext", "findByDn", "rename", "list", "listBindings"
            ),
            "org.apache.shiro.jndi.JndiTemplate" to setOf("lookup"),
            "org.springframework.jndi.JndiTemplate" to setOf("lookup")
        ),
        constructorSinks = setOf(
            "javax.management.remote.JMXServiceURL"
        ),
        isCall = true
    )

    private val ldapUnserializeDefinition = SubVulnerabilityDefinition(
        subType = SubVulnerabilityType.LDAP_UNSERIALIZE,
        methodSinks = mapOf(
            "javax.naming.directory.SearchControls" to setOf("setReturningObjFlag")
        ),
        constructorSinks = setOf("javax.naming.directory.SearchControls"),
        isCall = true
    )

    private val broadCORSAllowOriginDefinition = SubVulnerabilityDefinition(
        subType = SubVulnerabilityType.BROAD_CORES_ALLOW_ORIGIN,
        methodSinks = mapOf(
            "javax.servlet.http.HttpServletResponse" to setOf("setHeader", "addHeader"),
            "org.springframework.web.cors.CorsConfiguration" to setOf("addAllowedOrigin", "applyPermitDefaultValues"),
            "org.springframework.web.servlet.config.annotation.CorsRegistry" to setOf("addMapping")
        ),
        constructorSinks = emptySet(),
        isCall = false
    )

    private val hardcodesAllowOriginDefinition = SubVulnerabilityDefinition(
        subType = SubVulnerabilityType.HARDCODED_CREDENTIALS,
        methodSinks = mapOf(
            "java.util.Hashtable" to setOf("put"),
            "java.sql.DriverManager" to setOf("getConnection")
        ),
        constructorSinks = emptySet(),
        isCall = false
    )

    private val openSAML2IgnoreCommentDefinition = SubVulnerabilityDefinition(
        subType = SubVulnerabilityType.OPEN_SAML2_IGNORE_COMMENT,
        methodSinks = mapOf(
            "org.opensaml.xml.parse.StaticBasicParserPool" to setOf("setIgnoreComments"),
            "org.opensaml.xml.parse.BasicParserPool" to setOf("setIgnoreComments")
        ),
        constructorSinks = emptySet(),
        isCall = false
    )

    private val bshRceDefinition = SubVulnerabilityDefinition(
        subType = SubVulnerabilityType.BSHRCE,
        methodSinks = mapOf(
            "bsh.Interpreter" to setOf("eval"),
            "org.springframework.scripting.bsh.BshScriptEvaluator" to setOf("evaluate")
        ),
        constructorSinks = emptySet(),
        isCall = true
    )

    private val bURLAPIDefinition = SubVulnerabilityDefinition(
        subType = SubVulnerabilityType.BURLAP_UNSERIALIZE,
        methodSinks = mapOf(
            "com.caucho.burlap.io.BurlapInput" to setOf("readObject")
        ),
        constructorSinks = emptySet(),
        isCall = true
    )

    private val castorUnserializeDefinition = SubVulnerabilityDefinition(
        subType = SubVulnerabilityType.CASTOR_UNSERIALIZE,
        methodSinks = mapOf(
            "org.exolab.castor.xml.Marshaller" to setOf("marshal"),
            "org.exolab.castor.xml.Unmarshaller" to setOf("unmarshal")
        ),
        constructorSinks = emptySet(),
        isCall = true
    )

    private val compilableRceDefinition = SubVulnerabilityDefinition(
        subType = SubVulnerabilityType.COMPILABLE_RCE,
        methodSinks = mapOf(
            "javax.script.Compilable" to setOf("compile"),
            "javax.script.CompiledScript" to setOf("eval")
        ),
        constructorSinks = emptySet(),
        isCall = true
    )

    private val elRceDefinition = SubVulnerabilityDefinition(
        subType = SubVulnerabilityType.ELRCE,
        methodSinks = mapOf(
            "javax.el.ExpressionFactory" to setOf("createMethodExpression", "createValueExpression"),
            "javax.el.ELProcessor" to setOf("eval", "getValue"),
        ),
        constructorSinks = emptySet(),
        isCall = true
    )

    private val expressionRceDefinition = SubVulnerabilityDefinition(
        subType = SubVulnerabilityType.EXPRESSION_RCE,
        methodSinks = emptyMap(),
        constructorSinks = setOf("java.beans.Expression"),
        isCall = true
    )

    private val fastjsonUnserializeDefinition = SubVulnerabilityDefinition(
        subType = SubVulnerabilityType.FASTJSON_UNSERIALIZE,
        methodSinks = mapOf(
            "com.alibaba.fastjson.JSON" to setOf("parse", "parseObject"),
            "com.alibaba.fastjson.JSONObject" to setOf("parseObject"),
            "com.alibaba.fastjson.JSONArray" to setOf("parseArray"),
            "com.alibaba.fastjson.JSONObject" to setOf("parseObject"),
        ),
        constructorSinks = emptySet(),
        isCall = true
    )

    private val fastjsonAutoTypeDefinition = SubVulnerabilityDefinition(
        subType = SubVulnerabilityType.FASTJSON_AUTO_TYPE,
        methodSinks = mapOf(
            "com.alibaba.fastjson.parser.ParserConfig" to setOf("setAutoTypeSupport")
        ),
        constructorSinks = emptySet(),
        isCall = false
    )

    private val groovyRceDefinition = SubVulnerabilityDefinition(
        subType = SubVulnerabilityType.GROOVY_RCE,
        methodSinks = mapOf(
            "groovy.lang.GroovyClassLoader" to setOf("parseClass"),
            "groovy.lang.GroovyShell" to setOf("evaluate", "parse", "run"),
            "groovy.util.Eval" to setOf("me", "x", "xy", "xyz"),
            "org.codehaus.groovy.control.CompilationUnit" to setOf("compile"),
            "org.codehaus.groovy.tools.javac.JavaAwareCompilationUnit" to setOf("compile")
        ),
        constructorSinks = emptySet(),
        isCall = true
    )

    private val hessianUnserializeDefinition = SubVulnerabilityDefinition(
        subType = SubVulnerabilityType.HESSIAN_UNSERIALIZE,
        methodSinks = mapOf(
            "com.caucho.hessian.io.HessianInput" to setOf("readObject"),
            "com.caucho.hessian.io.Hessian2Input" to setOf("readObject")
        ),
        constructorSinks = emptySet(),
        isCall = true
    )

    private val jacksonRceDefinition = SubVulnerabilityDefinition(
        subType = SubVulnerabilityType.JACKSON_DATABIND_DEFAULT_TYPING,
        methodSinks = mapOf(
            "com.fasterxml.jackson.databind.ObjectMapper" to setOf("enableDefaultTyping"),
            "org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer" to setOf("setObjectMapper")
        ),
        constructorSinks = emptySet(),
        isCall = true
    )

    private val jexlRceDefinition = SubVulnerabilityDefinition(
        subType = SubVulnerabilityType.JEXLRCE,
        methodSinks = mapOf(
            "org.apache.commons.jexl3.Expression" to setOf("callable", "evaluate"),
            "org.apache.commons.jexl3.JexlEngine" to setOf("getProperty", "setProperty"),
            "org.apache.commons.jexl3.JexlExpression" to setOf("callable", "evaluate"),
            "org.apache.commons.jexl3.JexlScript" to setOf("callable", "evaluate"),
            "org.apache.commons.jexl3.JxltEngine\$Expression" to setOf("evaluate", "prepare"),
            "org.apache.commons.jexl3.JxltEngine\$Template" to setOf("evaluate"),
            "org.apache.commons.jexl3.Script" to setOf("callable", "execute"),
            "org.apache.commons.jexl2.Expression" to setOf("callable", "evaluate"),
            "org.apache.commons.jexl2.JexlEngine" to setOf("getProperty", "setProperty"),
            "org.apache.commons.jexl2.JexlExpression" to setOf("callable", "evaluate"),
            "org.apache.commons.jexl2.JexlScript" to setOf("callable", "execute"),
            "org.apache.commons.jexl2.Script" to setOf("callable", "execute"),
            "org.apache.commons.jexl2.UnifiedJEXL\$Expression" to setOf("evaluate", "prepare"),
            "org.apache.commons.jexl2.UnifiedJEXL\$Template" to setOf("evaluate")
        ),
        constructorSinks = emptySet(),
        isCall = true
    )

    private val jschOsRceDefinition = SubVulnerabilityDefinition(
        subType = SubVulnerabilityType.JSCH_OS_RCE,
        methodSinks = mapOf(
            "com.jcraft.jsch.JSch" to setOf(
                "getSession",
                "setCommand",
                "setPassword",
                "setConfig"
            )
        ),
        constructorSinks = emptySet(),
        isCall = true
    )

    private val jsonIOUnserializeDefinition = SubVulnerabilityDefinition(
        subType = SubVulnerabilityType.JSON_IO_UNSERIALIZE,
        methodSinks = mapOf(
            "com.cedarsoftware.util.io.JsonReader" to setOf("readObject")
        ),
        constructorSinks = emptySet(),
        isCall = true
    )

    private val jyamlUnserializeDefinition = SubVulnerabilityDefinition(
        subType = SubVulnerabilityType.JYAML_UNSERIALIZE,
        methodSinks = mapOf(
            "org.ho.yaml.Yaml" to setOf("load", "loadStream", "loadStreamOfType", "loadType"),
            "org.ho.yaml.YamlConfig" to setOf("load", "loadStream", "loadStreamOfType", "loadType")
        ),
        constructorSinks = emptySet(),
        isCall = true
    )

    private val jythonRceDefinition = SubVulnerabilityDefinition(
        subType = SubVulnerabilityType.JYTHON_RCE,
        methodSinks = mapOf(
            "org.python.util.PythonInterpreter" to setOf("eval", "exec")
        ),
        constructorSinks = emptySet(),
        isCall = true
    )

    private val kryoUnserializeDefinition = SubVulnerabilityDefinition(
        subType = SubVulnerabilityType.KRYO_UNSERIALIZE,
        methodSinks = mapOf(
            "com.esotericsoftware.kryo.Kryo" to setOf(
                "readObject",
                "readObjectOrNull",
                "readClassAndObject"
            )
        ),
        constructorSinks = emptySet(),
        isCall = true
    )

    private val mvelRceDefinition = SubVulnerabilityDefinition(
        subType = SubVulnerabilityType.MVELRCE,
        methodSinks = mapOf(
            "org.mvel2.MVEL" to setOf(
                "eval", "evalToBoolean", "evalToString",
                "executeAllExpression", "executeExpression", "executeSetExpression"
            ),
            "org.mvel2.MVELRuntime" to setOf("execute"),
            "org.mvel2.compiler.Accessor" to setOf("getValue"),
            "org.mvel2.compiler.CompiledAccExpression" to setOf("getValue"),
            "org.mvel2.compiler.CompiledExpression" to setOf("getDirectValue"),
            "org.mvel2.compiler.ExecutableStatement" to setOf("getValue"),
            "org.mvel2.jsr223.MvelCompiledScript" to setOf("eval"),
            "org.mvel2.jsr223.MvelScriptEngine" to setOf("eval", "evaluate"),
            "org.mvel2.templates.TemplateRuntime" to setOf("eval", "execute")
        ),
        constructorSinks = emptySet(),
        isCall = true
    )

    private val nashornScriptEngineRceDefinition = SubVulnerabilityDefinition(
        subType = SubVulnerabilityType.NASHORN_SCRIPT_ENGINE_RCE,
        methodSinks = mapOf(
            "jdk.nashorn.api.scripting.NashornScriptEngine" to setOf("eval")
        ),
        constructorSinks = emptySet(),
        isCall = true
    )

    private val objectInputStreamUnserializeDefinition = SubVulnerabilityDefinition(
        subType = SubVulnerabilityType.OBJECT_INPUT_STREAM_UNSERIALIZE,
        methodSinks = mapOf(
            "java.io.ObjectInputStream" to setOf("readObject")
        ),
        constructorSinks = emptySet(),
        isCall = true
    )

    private val ognlInjectionRceDefinition = SubVulnerabilityDefinition(
        subType = SubVulnerabilityType.OGNL_INJECTION_RCE,
        methodSinks = mapOf(
            "com.opensymphony.xwork2.ognl.OgnlUtil" to setOf("callMethod", "getValue", "setValue"),
            "ognl.Node" to setOf("getValue", "setValue"),
            "ognl.Ognl" to setOf("getValue", "setValue"),
            "ognl.enhance.ExpressionAccessor" to setOf("get", "set"),
            "org.apache.commons.ognl.Node" to setOf("getValue", "setValue"),
            "org.apache.commons.ognl.Ognl" to setOf("getValue", "setValue"),
            "org.apache.commons.ognl.enhance.ExpressionAccessor" to setOf("get", "set")
        ),
        constructorSinks = emptySet(),
        isCall = true
    )

    private val rhinoRceDefinition = SubVulnerabilityDefinition(
        subType = SubVulnerabilityType.RHINO_RCE,
        methodSinks = mapOf(
            "org.mozilla.javascript.Context" to setOf("evaluateString")
        ),
        constructorSinks = emptySet(),
        isCall = true
    )

    private val runtimeRceDefinition = SubVulnerabilityDefinition(
        subType = SubVulnerabilityType.RUNTIME_EXEC_RCE,
        methodSinks = mapOf(
            "java.lang.Runtime" to setOf("exec"),
            "java.lang.ProcessBuilder" to setOf()
        ),
        constructorSinks = setOf("java.lang.ProcessBuilder"),
        isCall = true
    )

    private val scriptEngineRceDefinition = SubVulnerabilityDefinition(
        subType = SubVulnerabilityType.SCRIPT_ENGINE_RCE,
        methodSinks = mapOf(
            "javax.script.ScriptEngine" to setOf("eval")
        ),
        constructorSinks = emptySet(),
        isCall = true
    )

    private val snakeYamlUnserializeDefinition = SubVulnerabilityDefinition(
        subType = SubVulnerabilityType.SNAKE_YAML_UNSERIALIZE,
        methodSinks = mapOf(
            "org.yaml.snakeyaml.Yaml" to setOf("load", "loadAll", "parse", "loadAs")
        ),
        constructorSinks = emptySet(),
        isCall = true
    )

    private val spelRceDefinition = SubVulnerabilityDefinition(
        subType = SubVulnerabilityType.SPEL_RCE,
        methodSinks = mapOf(
            "org.springframework.expression.ExpressionParser" to setOf("parseExpression"),
            "org.springframework.expression.spel.standard.SpelExpressionParser" to setOf(
                "parseExpression",
                "parseRaw"
            )
        ),
        constructorSinks = emptySet(),
        isCall = true
    )

    private val xmlDecoderUnserializeDefinition = SubVulnerabilityDefinition(
        subType = SubVulnerabilityType.XML_DECODER_UNSERIALIZE,
        methodSinks = mapOf(
            "java.beans.XMLDecoder" to setOf("readObject")
        ),
        constructorSinks = emptySet(),
        isCall = true
    )

    private val xsltRceDefinition = SubVulnerabilityDefinition(
        subType = SubVulnerabilityType.XSLT_RCE,
        methodSinks = mapOf(
            "javax.xml.transform.TransformerFactory" to setOf("newInstance")
        ),
        constructorSinks = emptySet(),
        isCall = true
    )

    private val xstreamUnserializeDefinition = SubVulnerabilityDefinition(
        subType = SubVulnerabilityType.XSTREAM_UNSERIALIZE,
        methodSinks = mapOf(
            "com.thoughtworks.xstream.XStream" to setOf("fromXML")
        ),
        constructorSinks = emptySet(),
        isCall = true
    )

    private val yamlBeansUnserializeDefinition = SubVulnerabilityDefinition(
        subType = SubVulnerabilityType.YAML_BEANS_UNSERIALIZE,
        methodSinks = mapOf(
            "com.esotericsoftware.yamlbeans.YamlReader" to setOf("read")
        ),
        constructorSinks = emptySet(),
        isCall = true
    )

    private val xxe = SubVulnerabilityDefinition(
        subType = SubVulnerabilityType.XXE,
        methodSinks = mapOf(
            "javax.xml.parsers.DocumentBuilderFactory" to setOf("newInstance"),
            "javax.xml.parsers.SAXParserFactory" to setOf("newInstance"),
            "javax.xml.transform.sax.SAXTransformerFactory" to setOf("newInstance"),
            "org.xml.sax.helpers.XMLReaderFactory" to setOf("createXMLReader"),
            "javax.xml.validation.SchemaFactory" to setOf("newInstance"),
            "javax.xml.stream.XMLInputFactory" to setOf("newFactory"),
            "javax.xml.transform.TransformerFactory" to setOf("newInstance"),
            "javax.xml.validation.Schema" to setOf("newValidator"),
            "org.apache.commons.digester3.Digester" to setOf("parse"),
            "org.dom4j.io.SAXReader" to setOf("read", "readDocument","setFeature"),
            "org.jdom.input.SAXBuilder" to setOf("build", "setFeature"),
        ),
        constructorSinks = emptySet(),
        isCall = true
    )

    private val javaSQLi = SubVulnerabilityDefinition(
        subType = SubVulnerabilityType.JAVA_SQLI,
        methodSinks = mapOf(
            "java.sql.Statement" to setOf("executeQuery"),
            "org.springframework.jdbc.core.JdbcTemplate" to setOf("query"),
            "java.sql.Connection" to setOf("prepareStatement")
        ),
        constructorSinks = emptySet(),
        isCall = true
    )

    private val jakartaRedirectDefinition = SubVulnerabilityDefinition(
        subType = SubVulnerabilityType.JAKARTA_REDIRECT,
        methodSinks = mapOf(
            "jakarta.ws.rs.core.Response" to setOf("seeOther", "temporaryRedirect")
        ),
        constructorSinks = emptySet(),
        isCall = true
    )

    private val javaxRedirectDefinition = SubVulnerabilityDefinition(
        subType = SubVulnerabilityType.JAVAX_REDIRECT,
        methodSinks = mapOf(
            "javax.servlet.http.HttpServletResponse" to setOf("sendRedirect"),
            "javax.servlet.RequestDispatcher" to setOf("getRequestDispatcher"),
            "javax.servlet.http.HttpServletRequest" to setOf("getRequestDispatcher"),
            "javax.ws.rs.core.Response" to setOf("seeOther", "temporaryRedirect")
        ),
        constructorSinks = setOf("org.springframework.web.servlet.ModelAndView"),
        isCall = true
    )

    private val reflectDefinition = SubVulnerabilityDefinition(
        subType = SubVulnerabilityType.REFLECT,
        methodSinks = mapOf(
            "java.lang.reflect.Method" to setOf("invoke"),
            "java.net.URLClassLoader" to setOf("newInstance"),
            "java.lang.ClassLoader" to setOf("loadClass"),
            "org.codehaus.groovy.runtime.InvokerHelper" to setOf("invokeMethod"),
            "groovy.lang.MetaClass" to setOf("invokeMethod", "invokeConstructor", "invokeStaticMethod")
        ),
        constructorSinks = emptySet(),
        isCall = true
    )


    private val apacheSSRFDefinition = SubVulnerabilityDefinition(
        subType = SubVulnerabilityType.APACHE_SSRF,
        methodSinks = mapOf(
            "org.apache.http.client.fluent.Request" to setOf("Get", "Post"),
            "org.apache.http.client.methods.HttpRequestBase" to setOf("setURI"),
            "org.apache.http.client.methods.RequestBuilder" to setOf(
                "get",
                "post",
                "put",
                "delete",
                "options",
                "head",
                "trace",
                "patch"
            )
        ),
        constructorSinks = setOf(
            "org.apache.commons.httpclient.methods.GetMethod",
            "org.apache.commons.httpclient.methods.PostMethod",
            "org.apache.http.client.methods.HttpGet",
            "org.apache.http.client.methods.HttpHead",
            "org.apache.http.client.methods.HttpPost",
            "org.apache.http.client.methods.HttpPut",
            "org.apache.http.client.methods.HttpDelete",
            "org.apache.http.client.methods.HttpOptions",
            "org.apache.http.client.methods.HttpTrace",
            "org.apache.http.client.methods.HttpPatch",
            "org.apache.http.message.BasicHttpRequest",
            "org.apache.http.message.BasicHttpEntityEnclosingRequest",
            "java.net.HttpURLConnection"
        ),
        isCall = true
    )

    private val googleIoSSRFDefinition = SubVulnerabilityDefinition(
        subType = SubVulnerabilityType.GOOGLE_IO_SSRF,
        methodSinks = mapOf(
            "com.google.common.io.Resources" to setOf(
                "asByteSource", "asCharSource", "copy", "readLines", "toByteArray", "toString"
            )
        ),
        constructorSinks = emptySet(),
        isCall = true
    )

    private val javaUrlSSRFDefinition = SubVulnerabilityDefinition(
        subType = SubVulnerabilityType.JAVA_URL_SSRF,
        methodSinks = mapOf(
            "java.net.URI" to setOf("create"),
            "java.net.URL" to setOf("openStream", "openConnection"),
            "java.net.Socket" to setOf("<init>"),
            "java.net.http.HttpRequest" to setOf("newBuilder"),
            "javax.ws.rs.client.Client" to setOf("target")
        ),
        constructorSinks = setOf(
            "java.net.URL",
            "java.net.Socket"
        ), isCall = true
    )

    private val jsoupSSRFDefinition = SubVulnerabilityDefinition(
        subType = SubVulnerabilityType.JSOUP_SSRF,
        methodSinks = mapOf(
            "org.jsoup.Jsoup" to setOf("connect"),
            "org.jsoup.Connection" to setOf("get", "post", "put", "delete", "options", "trace", "patch", "execute")
        ),
        constructorSinks = emptySet(),
        isCall = true
    )

    private val okHttpSSRFDefinition = SubVulnerabilityDefinition(
        subType = SubVulnerabilityType.OKHTTP_SSRF,
        methodSinks = mapOf(
            "com.squareup.okhttp.Request\$Builder" to setOf("url"),
            "okhttp3.Request\$Builder" to setOf("url"),
            "com.squareup.okhttp.Call" to setOf("execute"),
            "okhttp3.Call" to setOf("execute"),
            "com.squareup.okhttp.OkHttpClient" to setOf("newCall"),
            "okhttp3.OkHttpClient" to setOf("newCall"),
            "com.squareup.okhttp.Request" to setOf("get", "post", "put", "delete"),
            "okhttp3.Request" to setOf("get", "post", "put", "delete", "head", "patch")
        ),
        constructorSinks = emptySet(),
        isCall = true
    )

    private val springSSRFDefinition = SubVulnerabilityDefinition(
        subType = SubVulnerabilityType.SPRING_SSRF,
        methodSinks = mapOf(
            "org.springframework.web.reactive.function.client.WebClient" to setOf("create", "baseUrl"),
            "org.springframework.web.client.RestTemplate" to setOf(
                "getForEntity", "exchange", "execute", "getForObject",
                "postForEntity", "postForObject", "put", "headForHeaders",
                "optionsForAllow", "delete"
            ),
            "org.apache.http.client.HttpClient" to setOf("execute"),
            "org.apache.http.impl.client.CloseableHttpClient" to setOf("execute"),
            "java.net.URL" to setOf("openConnection"),
            "java.net.HttpURLConnection" to setOf("connect", "setRequestMethod")
        ),
        constructorSinks = setOf(
            "org.apache.http.client.methods.HttpGet",
            "org.apache.http.client.methods.HttpPost",
            "org.apache.http.client.methods.HttpPut",
            "org.apache.http.client.methods.HttpDelete",
            "java.net.URL"
        ),
        isCall = true
    )

    private val beetlSSTIDefinition = SubVulnerabilityDefinition(
        subType = SubVulnerabilityType.BEETL_SSTI,
        methodSinks = mapOf(
            "org.beetl.core.GroupTemplate" to setOf("getTemplate")
        ),
        constructorSinks = emptySet(),
        isCall = true
    )

    private val freemarkerSSTIDefinition = SubVulnerabilityDefinition(
        subType = SubVulnerabilityType.FREEMARKER_SSTI,
        methodSinks = mapOf(
            "freemarker.cache.StringTemplateLoader" to setOf("putTemplate")
        ),
        constructorSinks = setOf("freemarker.template.Template"),
        isCall = true
    )

    private val jinjavaSSTIDefinition = SubVulnerabilityDefinition(
        subType = SubVulnerabilityType.JINJAVA_SSTI,
        methodSinks = mapOf(
            "com.hubspot.jinjava.Jinjava" to setOf("render", "renderForResult")
        ),
        constructorSinks = emptySet(),
        isCall = true
    )

    private val pebbleSSTIDefinition = SubVulnerabilityDefinition(
        subType = SubVulnerabilityType.PEBBLE_SSTI,
        methodSinks = mapOf(
            "com.mitchellbosecke.pebble.PebbleEngine" to setOf("getLiteralTemplate", "getTemplate")
        ),
        constructorSinks = emptySet(),
        isCall = true
    )

    private val thymeleafSSTIDefinition = SubVulnerabilityDefinition(
        subType = SubVulnerabilityType.THYMELEAF_SSTI,
        methodSinks = mapOf(
            "org.thymeleaf.ITemplateEngine" to setOf("process", "processThrottled")
        ),
        constructorSinks = emptySet(),
        isCall = true
    )

    private val validationSSTIDefinition = SubVulnerabilityDefinition(
        subType = SubVulnerabilityType.VALIDATION_SSTI,
        methodSinks = mapOf(
            "javax.validation.ConstraintValidatorContext" to setOf("buildConstraintViolationWithTemplate"),
            "org.hibernate.validator.internal.engine.constraintvalidation.ConstraintValidatorContextImpl" to setOf("buildConstraintViolationWithTemplate")
        ),
        constructorSinks = emptySet(),
        isCall = true
    )

    private val velocitySSTIDefinition = SubVulnerabilityDefinition(
        subType = SubVulnerabilityType.VELOCITY_SSTI,
        methodSinks = mapOf(
            "org.apache.velocity.app.Velocity" to setOf("evaluate", "mergeTemplate"),
            "org.apache.velocity.app.VelocityEngine" to setOf("evaluate", "mergeTemplate"),
            "org.apache.velocity.runtime.RuntimeServices" to setOf("evaluate", "parse"),
            "org.apache.velocity.runtime.RuntimeSingleton" to setOf("parse"),
            "org.apache.velocity.runtime.resource.util.StringResourceRepository" to setOf("putStringResource")
        ),
        constructorSinks = emptySet(),
        isCall = true
    )

    val ALL_SUB_VUL_DEFINITIONS = setOf(
        nettyResponseSplittingDefinition,
        systemDosDefinition,
        pattenDosDefinition,
        readFileDefinition,
        commonsIODefinition,
        ioFiles,
        jdbcAttackDefinition,
        jndiInjectionDefinition,
        ldapUnserializeDefinition,
        broadCORSAllowOriginDefinition,
        hardcodesAllowOriginDefinition,
        openSAML2IgnoreCommentDefinition,
        bshRceDefinition,
        bURLAPIDefinition,
        castorUnserializeDefinition,
        compilableRceDefinition,
        elRceDefinition,
        expressionRceDefinition,
        fastjsonUnserializeDefinition,
        fastjsonAutoTypeDefinition,
        groovyRceDefinition,
        hessianUnserializeDefinition,
        jacksonRceDefinition,
        jexlRceDefinition,
        jschOsRceDefinition,
        jsonIOUnserializeDefinition,
        jyamlUnserializeDefinition,
        jythonRceDefinition,
        kryoUnserializeDefinition,
        mvelRceDefinition,
        nashornScriptEngineRceDefinition,
        objectInputStreamUnserializeDefinition,
        ognlInjectionRceDefinition,
        rhinoRceDefinition,
        runtimeRceDefinition,
        scriptEngineRceDefinition,
        snakeYamlUnserializeDefinition,
        spelRceDefinition,
        xmlDecoderUnserializeDefinition,
        xsltRceDefinition,
        xstreamUnserializeDefinition,
        yamlBeansUnserializeDefinition,
        jakartaRedirectDefinition,
        javaxRedirectDefinition,
        reflectDefinition,
        apacheSSRFDefinition,
        googleIoSSRFDefinition,
        javaUrlSSRFDefinition,
        jsoupSSRFDefinition,
        okHttpSSRFDefinition,
        springSSRFDefinition,
        beetlSSTIDefinition,
        freemarkerSSTIDefinition,
        jinjavaSSTIDefinition,
        pebbleSSTIDefinition,
        thymeleafSSTIDefinition,
        validationSSTIDefinition,
        velocitySSTIDefinition
    )

}