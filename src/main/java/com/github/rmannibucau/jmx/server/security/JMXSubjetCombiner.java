package com.github.rmannibucau.jmx.server.security;

import javax.security.auth.Subject;
import javax.security.auth.SubjectDomainCombiner;

public class JMXSubjetCombiner extends SubjectDomainCombiner {
    public JMXSubjetCombiner(final Subject subject) {
        super(subject);
    }
}
