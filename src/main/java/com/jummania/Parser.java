package com.jummania;

import com.jummania.reader.ByteReader;
import com.jummania.reader.Reader;
import com.jummania.writer.ByteWriter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public final class Parser {

    private final Serializer serializer = new Serializer();
    private final Deserializer deserializer = new Deserializer();
    ByteWriter sb = new ByteWriter();

    void main() throws Throwable {

        Company company = createCompany();

        for (int i = 0; i < 3; i++) {
            parse(company);
        }

    }

    void parse(Company company) throws Throwable {
        byte[] binary = serialize(company);
        deserializer.deserialize(Company.class, new ByteReader(binary));

        int limit = 9999;
        long start = System.nanoTime();

        for (int i = 0; i < limit; i++) {
            serialize(company);
        }

        long end = System.nanoTime();

        System.out.println((end - start) / limit);

        start = System.nanoTime();

        for (int i = 0; i < limit; i++) {
            deserializer.deserialize(Company.class, new ByteReader(binary));
        }

        end = System.nanoTime();

        System.out.println((end - start) / limit);
        System.out.println("lenths: " + binary.length);
    }

    public byte[] serialize(Object obj) throws Throwable {
        sb.reset();
        serializer.serialize(obj, Company.class, sb);
        return sb.toByteArray();
    }

    public Company createCompany() {

        Company company = new Company();

        company.id = 1;
        company.name = "Jummania Ltd";

        company.headOffice = new Address();
        company.headOffice.country = "Bangladesh";
        company.headOffice.city = "Dhaka";
        company.headOffice.street = "Motijheel";
        company.headOffice.zipCode = 1000;

        company.departments = new Department[5];

        for (int i = 0; i < 5; i++) {

            Department d = new Department();

            d.id = i;
            d.name = "Department " + i;
            d.active = true;

            company.departments[i] = d;
        }

        company.employees = new ArrayList<>();

        for (int i = 0; i < 100; i++) {

            Employee e = new Employee();

            e.id = i;
            e.name = "Employee " + i;
            e.age = 20 + (i % 30);
            e.salary = 25000 + i * 1000;

            e.address = new Address();
            e.address.country = "Bangladesh";
            e.address.city = "Dhaka";
            e.address.street = "Road " + i;
            e.address.zipCode = 1000 + i;

            e.phones = new ArrayList<>();

            for (int j = 0; j < 3; j++) {

                Phone p = new Phone();

                p.type = "Mobile";
                p.number = "01700000" + i + j;

                e.phones.add(p);
            }

            e.skills = new Skill[5];

            for (int j = 0; j < 5; j++) {

                Skill s = new Skill();

                s.name = "Skill-" + j;
                s.level = (j % 10) + 1;

                e.skills[j] = s;
            }

            company.employees.add(e);
        }

        return company;
    }

    public static class Address {

        public String country;
        public String city;
        public String street;
        public int zipCode;

        public static Address fromByte(byte[] sb) throws IOException {
            Reader reader = new ByteReader(sb);
            Address address = new Address();
            address.country = reader.readString();
            address.city = reader.readString();
            address.street = reader.readString();
            address.zipCode = reader.readInt();
            return address;

        }

        public byte[] toByte() {
            ByteWriter sb = new ByteWriter();
            sb.writeString(country);
            sb.writeString(city);
            sb.writeString(street);
            sb.writeInt(zipCode);
            return sb.toByteArray();
        }
    }

    public static class Employee {

        public long id;
        public String name;
        public int age;
        public double salary;

        public Address address;

        public List<Phone> phones;

        public Skill[] skills;

        public static Employee fromByte(byte[] bytes) throws IOException {
            Reader reader = new ByteReader(bytes);

            Employee employee = new Employee();

            employee.id = reader.readLong();
            employee.name = reader.readString();
            employee.age = reader.readInt();
            employee.salary = reader.readDouble();

            // Address
            if (reader.readBoolean()) {
                employee.address = Address.fromByte(reader.readBytes());
            }

            // Phones
            int phoneCount = reader.readInt();

            if (phoneCount >= 0) {
                employee.phones = new ArrayList<>(phoneCount);

                for (int i = 0; i < phoneCount; i++) {
                    employee.phones.add(Phone.fromByte(reader.readBytes()));
                }
            }

            // Skills
            int skillCount = reader.readInt();

            if (skillCount >= 0) {
                employee.skills = new Skill[skillCount];

                for (int i = 0; i < skillCount; i++) {
                    employee.skills[i] = Skill.fromByte(reader.readBytes());
                }
            }

            return employee;
        }

        public byte[] toByte() {
            ByteWriter sb = new ByteWriter();

            sb.writeLong(id);
            sb.writeString(name);
            sb.writeInt(age);
            sb.writeDouble(salary);

            // Address
            sb.writeBoolean(address != null);
            if (address != null) {
                sb.writeBytes(address.toByte());
            }

            // Phones
            if (phones == null) {
                sb.writeInt(-1);
            } else {
                sb.writeInt(phones.size());

                for (Phone phone : phones) {
                    sb.writeBytes(phone.toByte());
                }
            }

            // Skills
            if (skills == null) {
                sb.writeInt(-1);
            } else {
                sb.writeInt(skills.length);

                for (Skill skill : skills) {
                    sb.writeBytes(skill.toByte());
                }
            }

            return sb.toByteArray();
        }
    }

    public static class Phone {

        public String type;
        public String number;

        public static Phone fromByte(byte[] bytes) throws IOException {
            Reader reader = new ByteReader(bytes);

            Phone phone = new Phone();

            phone.type = reader.readString();
            phone.number = reader.readString();

            return phone;
        }

        public byte[] toByte() {
            ByteWriter sb = new ByteWriter();

            sb.writeString(type);
            sb.writeString(number);

            return sb.toByteArray();
        }
    }

    public static class Skill {

        public String name;
        public int level;

        public static Skill fromByte(byte[] bytes) throws IOException {
            Reader reader = new ByteReader(bytes);

            Skill skill = new Skill();

            skill.name = reader.readString();
            skill.level = reader.readInt();

            return skill;
        }

        public byte[] toByte() {
            ByteWriter sb = new ByteWriter();

            sb.writeString(name);
            sb.writeInt(level);

            return sb.toByteArray();
        }
    }

    public static class Company {

        public long id;
        public String name;

        public Address headOffice;

        public Department[] departments;

        public List<Employee> employees;

        public static Company fromByte(byte[] bytes) throws IOException {
            Reader reader = new ByteReader(bytes);

            Company company = new Company();

            company.id = reader.readLong();
            company.name = reader.readString();

            // Head Office
            if (reader.readBoolean()) {
                company.headOffice = Address.fromByte(reader.readBytes());
            }

            // Departments
            int departmentCount = reader.readInt();

            if (departmentCount >= 0) {
                company.departments = new Department[departmentCount];

                for (int i = 0; i < departmentCount; i++) {
                    company.departments[i] = Department.fromByte(reader.readBytes());
                }
            }

            // Employees
            int employeeCount = reader.readInt();

            if (employeeCount >= 0) {
                company.employees = new ArrayList<>(employeeCount);

                for (int i = 0; i < employeeCount; i++) {
                    company.employees.add(Employee.fromByte(reader.readBytes()));
                }
            }

            return company;
        }

        public byte[] toByte() {
            ByteWriter sb = new ByteWriter();

            sb.writeLong(id);
            sb.writeString(name);

            // Head Office
            sb.writeBoolean(headOffice != null);
            if (headOffice != null) {
                sb.writeBytes(headOffice.toByte());
            }

            // Departments
            if (departments == null) {
                sb.writeInt(-1);
            } else {
                sb.writeInt(departments.length);

                for (Department department : departments) {
                    sb.writeBytes(department.toByte());
                }
            }

            // Employees
            if (employees == null) {
                sb.writeInt(-1);
            } else {
                sb.writeInt(employees.size());

                for (Employee employee : employees) {
                    sb.writeBytes(employee.toByte());
                }
            }

            return sb.toByteArray();
        }
    }

    public static class Department {

        public int id;
        public String name;
        public boolean active;

        public static Department fromByte(byte[] bytes) throws IOException {
            Reader reader = new ByteReader(bytes);

            Department department = new Department();

            department.id = reader.readInt();
            department.name = reader.readString();
            department.active = reader.readBoolean();

            return department;
        }

        public byte[] toByte() {
            ByteWriter sb = new ByteWriter();

            sb.writeInt(id);
            sb.writeString(name);
            sb.writeBoolean(active);

            return sb.toByteArray();
        }
    }
}
