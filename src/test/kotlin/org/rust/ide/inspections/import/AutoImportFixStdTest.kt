/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.inspections.import

import com.intellij.testFramework.LightProjectDescriptor
import org.rust.ide.inspections.fixes.import.AutoImportFix

class AutoImportFixStdTest : AutoImportFixTestBase() {

    override fun getProjectDescriptor(): LightProjectDescriptor = WithStdlibAndDependencyRustProjectDescriptor

    fun `test import item from std crate`() = checkAutoImportFixByText("""
        fn foo<T: <error descr="Unresolved reference: `io`">io::Read/*caret*/</error>>(t: T) {}
    """, """
        use std::io;

        fn foo<T: io::Read/*caret*/>(t: T) {}
    """, AutoImportFix.Testmarks.autoInjectedStdCrate)

    fun `test import item from not std crate`() = checkAutoImportFixByFileTree("""
        //- dep-lib/lib.rs
        pub mod foo {
            pub struct Bar;
        }
        //- main.rs
        fn foo(t: <error descr="Unresolved reference: `Bar`">Bar/*caret*/</error>) {}
    """, """
        //- main.rs
        extern crate dep_lib_target;

        use dep_lib_target::foo::Bar;

        fn foo(t: Bar/*caret*/) {}
    """)

    fun `test don't insert extern crate item it is already exists`() = checkAutoImportFixByFileTree("""
        //- dep-lib/lib.rs
        pub mod foo {
            pub struct Bar;
        }
        //- main.rs
        extern crate dep_lib_target;

        fn foo(t: <error descr="Unresolved reference: `Bar`">Bar/*caret*/</error>) {}
    """, """
        //- main.rs
        extern crate dep_lib_target;

        use dep_lib_target::foo::Bar;

        fn foo(t: Bar/*caret*/) {}
    """)

    fun `test insert new extern crate item after existing extern crate items`() = checkAutoImportFixByFileTree("""
        //- dep-lib/lib.rs
        pub mod foo {
            pub struct Bar;
        }
        //- main.rs
        extern crate std;

        fn foo(t: <error descr="Unresolved reference: `Bar`">Bar/*caret*/</error>) {}
    """, """
        //- main.rs
        extern crate std;
        extern crate dep_lib_target;

        use dep_lib_target::foo::Bar;

        fn foo(t: Bar/*caret*/) {}
    """)

    fun `test insert extern crate item after inner attributes`() = checkAutoImportFixByFileTree("""
        //- main.rs
        #![allow(non_snake_case)]

        fn foo(t: <error descr="Unresolved reference: `Bar`">Bar/*caret*/</error>) {}

        //- dep-lib/lib.rs
        pub mod foo {
            pub struct Bar;
        }
    """, """
        //- main.rs
        #![allow(non_snake_case)]

        extern crate dep_lib_target;

        use dep_lib_target::foo::Bar;

        fn foo(t: Bar/*caret*/) {}
    """)

    fun `test import reexported item from stdlib`() = checkAutoImportFixByText("""
        fn main() {
            let mutex = <error descr="Unresolved reference: `Mutex`">Mutex/*caret*/::new</error>(Vec::new());
        }
    """, """
        use std::sync::Mutex;

        fn main() {
            let mutex = Mutex/*caret*/::new(Vec::new());
        }
    """, AutoImportFix.Testmarks.autoInjectedStdCrate)

    fun `test module reexport`() = checkAutoImportFixByFileTree("""
        //- dep-lib/lib.rs
        pub mod foo {
            mod bar {
                pub mod baz {
                    pub struct FooBar;
                }
            }

            pub use self::bar::baz;
        }

        //- main.rs
        fn main() {
            let x = <error descr="Unresolved reference: `FooBar`">FooBar/*caret*/</error>;
        }
    """, """
        //- main.rs
        extern crate dep_lib_target;

        use dep_lib_target::foo::baz::FooBar;

        fn main() {
            let x = FooBar/*caret*/;
        }
    """)

    fun `test module reexport in stdlib`() = checkAutoImportFixByText("""
        fn foo<T: <error descr="Unresolved reference: `Hash`">Hash/*caret*/</error>>(t: T) {}
    """, """
        use std::hash::Hash;

        fn foo<T: Hash/*caret*/>(t: T) {}
    """, AutoImportFix.Testmarks.autoInjectedStdCrate)

    fun `test import without std crate 1`() = checkAutoImportFixByText("""
        #![no_std]

        fn foo<T: <error descr="Unresolved reference: `Hash`">Hash/*caret*/</error>>(t: T) {}
    """, """
        #![no_std]

        use core::hash::Hash;

        fn foo<T: Hash/*caret*/>(t: T) {}
    """, AutoImportFix.Testmarks.autoInjectedCoreCrate)

    fun `test import without std crate 2`() = checkAutoImportFixByText("""
        #![no_std]

        fn main() {
            let x = <error descr="Unresolved reference: `Arc`">Arc::new/*caret*/</error>(123);
        }
    """, """
        #![no_std]

        extern crate alloc;

        use alloc::arc::Arc;

        fn main() {
            let x = Arc::new/*caret*/(123);
        }
    """)
}
