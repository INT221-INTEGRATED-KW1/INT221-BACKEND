package sit.int221.integratedproject.kanbanborad.config;

import jakarta.persistence.EntityManagerFactory;
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
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;


@Configuration
@EnableTransactionManagement
@EnableJpaRepositories(
        basePackages = "sit.int221.integratedproject.kanbanborad.repositories.itbkkshared",
        entityManagerFactoryRef = "itbkksharedEntityManagerFactory",
        transactionManagerRef = "itbkksharedTransactionManager"
)
public class itbkksharedDataSourceConfig {
    @Bean(name = "itbkksharedDataSource")
    @ConfigurationProperties(prefix = "spring.datasource.itbkkshared")
    public DataSource itbkk_sharedDataSource() {
        return DataSourceBuilder.create().build();
    }

    @Bean(name = "itbkksharedEntityManagerFactory")
    public LocalContainerEntityManagerFactoryBean testUserEntityManagerFactory(
            EntityManagerFactoryBuilder builder,
            @Qualifier("itbkksharedDataSource") DataSource testUserDataSource) {
        return builder
                .dataSource(testUserDataSource)
                .packages("sit.int221.integratedproject.kanbanborad.entities")
                .persistenceUnit("itbkkshared")
                .build();
    }

    @Bean(name = "itbkksharedTransactionManager")
    public PlatformTransactionManager testUserTransactionManager(
            @Qualifier("itbkksharedEntityManagerFactory") EntityManagerFactory testUserEntityManagerFactory) {
        return new JpaTransactionManager(testUserEntityManagerFactory);
    }
}
