package TCBot;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class TeleCordProps extends Properties{
    private static TeleCordProps instance = null;
    
    private TeleCordProps(){
        
    }
    
    public static TeleCordProps getInstance(){
        if (instance == null){
            try {
                instance = new TeleCordProps();
                System.out.println(new File(".").getAbsolutePath());
                InputStream fis = TeleCordProps.class.getResourceAsStream("/telecord.properties");
                instance.load(fis);
                fis.close();
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        }
        return instance;
    }
}
