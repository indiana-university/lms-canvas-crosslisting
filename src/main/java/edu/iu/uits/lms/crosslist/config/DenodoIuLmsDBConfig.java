package edu.iu.uits.lms.crosslist.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.transaction.PlatformTransactionManager;

import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

@Configuration("denodoIuLmsDbConfig")
@EnableJpaRepositories(
        entityManagerFactoryRef = "denodoIuLmsDbEntityMgrFactory",
        transactionManagerRef = "denodoIuLmsDbTransactionMgr",
        basePackages = {
                "edu.iu.uits.lms.crosslist.model"
        })
@Slf4j
public class DenodoIuLmsDBConfig {

    @Bean(name = "denodoIuLmsDb")
    @ConfigurationProperties(prefix = "spring.datasource.batchdenodoiulms")
    public DataSource dataSource() {
        return DataSourceBuilder.create().build();
    }

    @Bean(name = "denodoIuLmsDbEntityMgrFactory")
    public LocalContainerEntityManagerFactoryBean denodoIuLmsDbEntityMgrFactory(
            final EntityManagerFactoryBuilder builder,
            @Qualifier("denodoIuLmsDb") final DataSource dataSource) {
        // dynamically setting up the hibernate properties for each of the datasource.
        final Map<String, String> properties = new HashMap<>();
        return builder
                .dataSource(dataSource)
                .properties(properties)
                .packages("edu.iu.uits.lms.crosslist.model")
                .build();
    }

    @Bean(name = "denodoIuLmsDbTransactionMgr")
    public PlatformTransactionManager denodoIuLmsDbTransactionMgr(
            @Qualifier("denodoIuLmsDbEntityMgrFactory") final EntityManagerFactory entityManagerFactory) {
        return new JpaTransactionManager(entityManagerFactory);
    }
}
