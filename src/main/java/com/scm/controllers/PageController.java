package com.scm.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import com.scm.entities.User;
import com.scm.forms.UserForm;
import com.scm.helpers.Message;
import com.scm.helpers.MessageType;
import com.scm.services.UserService;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import java.util.*;

import com.scm.repsitories.UserRepo;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Controller
public class PageController {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepo userRepo;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private com.scm.helpers.Helper helper;

    @GetMapping("/")
    public String index() {
        return "redirect:/home";
    }

    @RequestMapping("/home")
    public String home(Model model) {
        System.out.println("Home page handler");
        // sending data to view
        model.addAttribute("name", "Substring Technologies");
        model.addAttribute("youtubeChannel", "Learn Code With Durgesh");
        model.addAttribute("githubRepo", "https://github.com/learncodewithdurgesh/");
        return "home";
    }

    // about route

    @RequestMapping("/about")
    public String aboutPage(Model model) {
        model.addAttribute("isLogin", true);
        System.out.println("About page loading");
        return "about";
    }

    // services

    @RequestMapping("/services")
    public String servicesPage() {
        System.out.println("services page loading");
        return "services";
    }

    // contact page

    @GetMapping("/contact")
    public String contact() {
        return new String("contact");
    }

    // this is showing login page
    @GetMapping("/login")
    public String login() {
        return new String("login");
    }

    // registration page
    @GetMapping("/register")
    public String register(Model model) {

        UserForm userForm = new UserForm();
        // default data bhi daal sakte hai
        // userForm.setName("Durgesh");
        // userForm.setAbout("This is about : Write something about yourself");
        model.addAttribute("userForm", userForm);

        return "register";
    }

    // processing register

    @RequestMapping(value = "/do-register", method = RequestMethod.POST)
    public String processRegister(@Valid @ModelAttribute UserForm userForm, BindingResult rBindingResult,
            HttpSession session) {
        System.out.println("Processing registration");
        // fetch form data
        // UserForm
        System.out.println(userForm);

        // validate form data
        if (rBindingResult.hasErrors()) {
            return "register";
        }

        // Check if user already exists
        if (userService.isUserExistByEmail(userForm.getEmail())) {
            Message message = Message.builder().content("User already exists with this email").type(MessageType.red)
                    .build();
            session.setAttribute("message", message);
            return "register";
        }

        try {
            User user = new User();
            user.setName(userForm.getName());
            user.setEmail(userForm.getEmail());
            user.setPassword(userForm.getPassword());
            user.setAbout(userForm.getAbout());
            user.setPhoneNumber(userForm.getPhoneNumber());
            user.setEnabled(false);
            user.setProfilePic(
                    "https://www.learncodewithdurgesh.com/_next/image?url=%2F_next%2Fstatic%2Fmedia%2Fdurgesh_sir.35c6cb78.webp&w=1920&q=75");

            User savedUser = userService.saveUser(user);

            System.out.println("user saved :");

            // add the message:

            String verificationLink = helper.getLinkForEmailVerificatiton(savedUser.getEmailToken());
            Message message = Message.builder()
                    .content("Registration Successful. Verification Link (Dev): <a href=\"" + verificationLink
                            + "\">Click Here</a>")
                    .type(MessageType.green).build();

            session.setAttribute("message", message);

            // redirectto login page
            return "redirect:/register";
        } catch (Exception e) {
            // handle exception if email sending fails or other issues
            Message message = Message.builder().content(
                    "Something went wrong: " + e.getMessage())
                    .type(MessageType.red).build();
            session.setAttribute("message", message);
            return "register";
        }
    }

    // Guest login handler
    @GetMapping("/guest-login")
    public String guestLogin(HttpSession session) {
        try {
            // Create or retrieve guest user
            String guestEmail = "guest@contactcore.local";
            User guestUser = userService.getUserByEmail(guestEmail);

            if (guestUser == null) {
                // Guest user doesn't exist, create it
                guestUser = new User();
                guestUser.setUserId(UUID.randomUUID().toString()); // Generate ID manually
                guestUser.setName("Guest User");
                guestUser.setEmail(guestEmail);
                guestUser.setPassword(passwordEncoder.encode("guest123")); // Single encoding
                guestUser.setAbout("Temporary guest user account with full access.");
                guestUser.setPhoneNumber("0000000000");
                guestUser.setEnabled(true);
                guestUser.setEmailVerified(true);
                guestUser.setPhoneVerified(true);
                guestUser.setProfilePic("/images/guest_profile.png");

                // Grant full rights
                guestUser.setRoleList(new ArrayList<>(List.of("ROLE_GUEST", "ROLE_USER")));

                // Save directly using repo to avoid email sending and role overrides
                guestUser = userRepo.save(guestUser);
            }

            // Authenticate the guest user
            Authentication authentication = new UsernamePasswordAuthenticationToken(
                    guestUser, null, guestUser.getAuthorities());
            SecurityContextHolder.getContext().setAuthentication(authentication);
            session.setAttribute("SPRING_SECURITY_CONTEXT", SecurityContextHolder.getContext());

            // Add success message
            Message message = Message.builder()
                    .content("Welcome, Guest! You have full access to the dashboard.")
                    .type(MessageType.green)
                    .build();
            session.setAttribute("message", message);

            return "redirect:/user/dashboard";
        } catch (Exception e) {
            e.printStackTrace();
            Message message = Message.builder()
                    .content("Could not log in as guest: " + e.getMessage())
                    .type(MessageType.red)
                    .build();
            session.setAttribute("message", message);
            return "redirect:/login";
        }
    }

}
