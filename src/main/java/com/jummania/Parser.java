package com.jummania;

import com.jummania.writer.ByteWriter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class Parser {

    private final Serializer serializer = new Serializer();
    private final Deserializer deserializer = new Deserializer();
    ByteWriter sb = new ByteWriter();

    void main() throws Throwable {

        //GeneratedSerializers.serialize(new TT(), sb);

        System.out.println(sb.toHexString());
        if (true) return;

        Company company = createCompany();

        System.out.println("lenths: " + serialize(company).length);
        //  System.out.println("lenths: " + company.toByte().length);
        for (int i = 0; i < 3; i++) {
            parse(company);
        }

    }

    void parse(Company company) throws Throwable {

        int limit = 9999;
        long start = System.nanoTime();

        for (int i = 0; i < limit; i++) {
            serialize(company);
        }

        long end = System.nanoTime();

        System.out.println((end - start) / limit);

        start = System.nanoTime();

        for (int i = 0; i < limit; i++) {
            //  company.toByte();
        }

        end = System.nanoTime();

        System.out.println((end - start) / limit);
    }

    public byte[] serialize(Company obj) throws Throwable {
        sb.reset();
        //   GeneratedSerializers.serialize(obj, sb);
        // serializer.serialize(obj, Company.class, sb);
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

    // @Serializable
    public static class Address {

        public String country;
        public String city;
        public String street;
        public int zipCode;
    }

    // @Serializable
    public static class Employee {

        public long id;
        public String name;
        public int age;
        public double salary;

        public Address address;

        public List<Phone> phones;

        public Skill[] skills;
    }

    // @Serializable
    public static class Phone {

        public String type;
        public String number;

    }

    //  @Serializable
    public static class Skill {

        public String name;
        public int level;
    }

    //  @Serializable
    public static class Company {

        // fd
        public long id;
        public String name;

        public Address headOffice;

        public Department[] departments;

        public List<Employee> employees;
    }

    @Serializable
    public static class Department {

        private int id;
        public String name;
        public boolean active;
    }


    // @Serializable
    public class TT {

        UserProfile[] userProfile;

        //Iterable<String> itemListener;
        // ১. মাল্টি-লেভেল নেস্টেড লিস্ট ও ম্যাপের কম্বিনেশন
        List<Map<String, List<Map<Integer, Company>>>> complexNestedField;

        // ২. অ্যারে এবং নেস্টেড ম্যাপ
        Map<String, String[]>[] mapArrayField;

        // ৩. থ্রি-ডি বা ডিপ নেস্টেড লিস্ট
        List<List<List<Integer>>> deepListField;

        // ৪. কাস্টম অবজেক্ট নেস্টিং (যদি আপনার সিস্টেমে কাস্টম ক্লাস হ্যান্ডলিং থাকে)
        Map<String, UserProfile> userProfileMap;


        //   Company company;

        public static class UserProfile {
            String title;
            String[] arrayId;
            public String username;
            public List<String> tags;
            public Map<String, Integer> metadata;
        }
    }

    //  @Serializable
    public class TestConflict {
        public int id; // রুট লেভেলে id
        public String name; // রুট লেভেলে name

        public InnerData innerData; // প্রথম লেভেলে নেস্টেড অবজেক্ট
        public Map<String, InnerData> innerDataMap; // ম্যাপ যার ভ্যালুতেও InnerData আছে

        public static class InnerData {
            public int id; // কনফ্লিক্ট করার জন্য একই নাম 'id'
            public String name; // কনফ্লিক্ট করার জন্য একই নাম 'name'

            public List<String> tags; // লিস্ট ফিল্ড
            public Map<String, Integer> metadata; // ম্যাপ ফিল্ড

            public DeepInner deepInner; // আরও গভীরের নেস্টেড অবজেক্ট
        }

        public static class DeepInner {
            public int id;
        }
    }
}
