/*
 * LASSO - an Observatorium for the Dynamic Selection, Analysis and Comparison of Software
 * Copyright (C) 2024 Marcus Kessel (University of Mannheim) and LASSO contributers
 *
 * This file is part of LASSO.
 *
 * LASSO is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * LASSO is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with LASSO.  If not, see <https://www.gnu.org/licenses/>.
 */
package de.uni_mannheim.swt.lasso.service.app;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.uni_mannheim.swt.lasso.service.persistence.User;
import org.apache.commons.io.IOUtils;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 *
 * @author Marcus Kessel
 */
public class UserManagementUtils {

    public static List<User> read(InputStream in, PasswordEncoder passwordEncoder) throws IOException {
        ObjectMapper mapper = new ObjectMapper();

        List<User> users = mapper.readValue(in, new TypeReference<List<User>>(){});

        IOUtils.closeQuietly(in);

        // encrypt password
        users.forEach(u -> u.setPassword(passwordEncoder.encode(u.getPassword())));

        return users;
    }
}
