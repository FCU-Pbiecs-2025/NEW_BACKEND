package Group4.Childcare.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import java.io.File;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 取得應用程式運行目錄
        String currentDir = System.getProperty("user.dir");
        System.out.println("Current Working Directory: " + currentDir);

        // 建構資料夾路徑
        String institutionDir = new File(currentDir, "InstitutionResource").getAbsolutePath();
        String bannerDir = new File(currentDir, "BannerResource").getAbsolutePath();
        String attachmentDir = new File(currentDir, "AttachmentResource").getAbsolutePath();
        String identityDir = new File(currentDir, "IdentityResource").getAbsolutePath();

        System.out.println("=== Resource Directories ===");
        System.out.println("Institution: " + institutionDir);
        System.out.println("Banner: " + bannerDir);
        System.out.println("Attachment: " + attachmentDir);
        System.out.println("Identity: " + identityDir);

        // 檢查目錄是否存在
        System.out.println("Institution exists: " + new File(institutionDir).exists());
        System.out.println("Banner exists: " + new File(bannerDir).exists());
        System.out.println("Attachment exists: " + new File(attachmentDir).exists());
        System.out.println("Identity exists: " + new File(identityDir).exists());

        // 映射靜態資源
        // file:/// 協議用於本地檔案系統
        registry.addResourceHandler("/institution-files/**")
                .addResourceLocations("file:///" + institutionDir.replace("\\", "/") + "/");

        registry.addResourceHandler("/banner-files/**")
                .addResourceLocations("file:///" + bannerDir.replace("\\", "/") + "/");

        registry.addResourceHandler("/attachment-files/**")
                .addResourceLocations("file:///" + attachmentDir.replace("\\", "/") + "/");

        registry.addResourceHandler("/identity-files/**")
                .addResourceLocations("file:///" + identityDir.replace("\\", "/") + "/");
    }
}

