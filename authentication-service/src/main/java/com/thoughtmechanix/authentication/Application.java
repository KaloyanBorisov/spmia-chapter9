package com.thoughtmechanix.authentication;

import com.thoughtmechanix.authentication.model.UserInfo;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableAuthorizationServer;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableResourceServer;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@SpringBootApplication
@RestController
@EnableResourceServer
@EnableAuthorizationServer
public class Application {

    //curl  eagleeye:thisissecret@192.168.99.100:8901/auth/oauth/token -d grant_type=password -client_id=eagleeye  -d scope=webclient -d username=william.woodward -d password=password2

    //curl -H "Authorization: Bearer 24fb9e40-21d4-4e78-b211-92edfa2fc514" http://192.168.99.100:8901/auth/user
    @RequestMapping(value = { "/user" }, produces = "application/json")
    public Map<String, Object> user(OAuth2Authentication user) {
        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("user", user.getUserAuthentication().getPrincipal());
        userInfo.put("authorities", AuthorityUtils.authorityListToSet(user.getUserAuthentication().getAuthorities()));
        return userInfo;
    }

/*    @GetMapping( value = "/validate/{token}")
    public ResponseEntity<UserInfo>  validate(@PathVariable("token") String token){
        UserInfo userInfo = new UserInfo();
        userInfo.setUserId("william.woodward");
        userInfo.setOrganizationId("42d3d4f5-9f33-42f4-8aca-b7519d6af1bb");
        return new ResponseEntity<>(userInfo, HttpStatus.OK);
    }*/

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }


}
