import java.nio.file.Files
import java.nio.file.StandardCopyOption

def afterBuildTask (String verName, String vc, String bc) {
    int verCode = Integer.parseInt(vc)
    int buildCode = Integer.parseInt(bc);
    println "Version name: " + verName
    println "Version code: " + verCode.toString()
    println "Build code: " + buildCode.toString()
    def apk = new File('F:\\AndroidStudio\\Documenter\\app\\release\\app-release.apk')
    def newName = "documenter." + verName + ".apk"
    //def destFile1 = new File('F:\\AndroidStudio\\Documenter\\app\\release\\' + newName)
    def destFile2 = new File('d:\\Sites\\res.maxsavteam.com\\apk\\documenter\\' + newName)
    println apk.absolutePath
    //println destFile1.absolutePath
    println destFile2.absolutePath
    /*if(!destFile1.exists())
        destFile1.createNewFile()*/

    if(!destFile2.exists())
        destFile2.createNewFile()
    if(apk.exists()) {
        println "copy operation"
        //Files.copy(apk.toPath(), destFile1.toPath(), StandardCopyOption.REPLACE_EXISTING)
        Files.copy(apk.toPath(), destFile2.toPath(), StandardCopyOption.REPLACE_EXISTING)
    }else{
        println "source apk not found"
        return
    }

    def verFile = new File('d:\\GitHub\\mstresources.github.io\\apk\\documenter\\version')
    def verFileJSon = new File('d:\\Sites\\res.maxsavteam.com\\apk\\documenter\\version.json')
    if(!verFile.exists())
        verFile.createNewFile()
    if(!verFileJSon.exists())
        verFileJSon.createNewFile()

    String downloadUrl = "https://res.maxsavteam.com/apk/documenter/" + newName
    String endStr = verCode.toString() + ";" + downloadUrl + ";" + verName + ";" + destFile2.length().toString() + ";" + buildCode + ";"
    boolean isNecessary = false // change it when release new update, if need
    String contentJson = """{
    "downloadUrl": "${downloadUrl}",
    "versionCode": ${verCode},
    "versionName": "${verName}",
    "apkSize": ${destFile1.length()},
    "buildCode" : ${buildCode},
    "necessaryUpdate": ${isNecessary},
    "minSdk": 23
}"""

    println contentJson
    println "New content: " + endStr
    println "Previous content: " + verFile.text
    println "Saving previous..."
    def file = new File('previous_update.txt')
    file.write(verFile.text)
    println "Writing to version file..."
    verFileJSon.write(contentJson)
    verFile.write(endStr)

    println "After build task successfully completed"
}

def rollBack(){
    def file = new File('previous_update.txt')
    println "Text to be restored is " + file.text
    def verFile = new File('d:\\GitHub\\mstresources.github.io\\apk\\documenter\\version')
    verFile.write(file.text)
    println "Successfully"
}

static main( args ) {
    if( args ) {
        "${args.head()}"( *args.tail() )
    }
}