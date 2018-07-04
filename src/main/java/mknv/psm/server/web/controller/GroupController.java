package mknv.psm.server.web.controller;

import mknv.psm.server.model.domain.Group;
import mknv.psm.server.model.domain.User;
import mknv.psm.server.model.repository.GroupRepository;
import mknv.psm.server.web.exception.ControllerSecurityException;
import mknv.psm.server.web.exception.EntityNotFoundException;
import java.util.List;
import javax.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.propertyeditors.StringTrimmerEditor;
import org.springframework.context.MessageSource;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import mknv.psm.server.model.repository.UserRepository;

/**
 *
 * @author mknv
 */
@Controller
public class GroupController {

    @Autowired
    private GroupRepository groupRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private MessageSource messageSource;

    @InitBinder
    public void init(WebDataBinder binder) {
        binder.registerCustomEditor(String.class, new StringTrimmerEditor(false));
    }

    @ModelAttribute("groups")
    public List<Group> groups(Model model, Authentication authentication) {
        User currentUser = userRepository.findByName(authentication.getName());
        List<Group> groups = groupRepository.findByUser(currentUser);
        return groups;
    }

    @GetMapping("/groups")
    public String list() {
        return "groups/list";
    }

    @GetMapping("/groups/create")
    public String prepareCreate(Model model) {
        model.addAttribute("group", new Group());
        return "groups/create";
    }

    @PostMapping("/groups/create")
    public String create(@Valid @ModelAttribute Group group, BindingResult bindingResult,
            Authentication authentication, Model model) {
        if (bindingResult.hasErrors()) {
            return "groups/create";
        }
        group.setUser(userRepository.findByName(authentication.getName()));
        try {
            groupRepository.save(group);
        } catch (DataIntegrityViolationException ex) {
            model.addAttribute("error", messageSource.getMessage("group.exists", null, null));
            return "groups/create";
        }
        return "redirect:/groups";
    }

    @GetMapping("/groups/edit/{id}")
    public String prepareEdit(@PathVariable("id") Integer id, Model model, Authentication authentication) {
        Group group = groupRepository.findByIdFetchUser(id);
        if (group == null) {
            throw new EntityNotFoundException(Group.class, id);
        }
        if (!group.getUser().getName().equals(authentication.getName())) {
            throw new ControllerSecurityException();
        }
        model.addAttribute("group", group);
        return "groups/edit";
    }

    @PostMapping("/groups/update")
    public String update(@Valid @ModelAttribute Group group, BindingResult bindingResult,
            Authentication authentication, Model model) {
        if (bindingResult.hasErrors()) {
            return "groups/edit";
        }
        Group existingGroup = groupRepository.findByIdFetchUser(group.getId());
        if (!existingGroup.getUser().getName().equals(authentication.getName())) {
            throw new ControllerSecurityException();
        }
        group.setUser(existingGroup.getUser());
        try {
            groupRepository.save(group);
        } catch (DataIntegrityViolationException e) {
            model.addAttribute("error", messageSource.getMessage("group.exists", null, null));
            return "groups/edit";
        }
        return "redirect:/groups";
    }

    @PostMapping("/groups/delete/{id}")
    public String delete(@PathVariable("id") Integer id, Model model, Authentication authentication) {
        Group group = groupRepository.findByIdFetchUser(id);
        if (group == null) {
            throw new EntityNotFoundException(Group.class, id);
        }
        if (!group.getUser().getName().equals(authentication.getName())) {
            throw new ControllerSecurityException();
        }
        try {
            groupRepository.deleteById(id);
        } catch (DataIntegrityViolationException e) {
            model.addAttribute("error", messageSource.getMessage("group.entries.constraint", null, null));
            return "groups/list";
        }
        return "redirect:/groups";
    }
}
