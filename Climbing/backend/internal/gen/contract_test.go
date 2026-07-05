package gen_test

import (
	"go/ast"
	"go/parser"
	"go/token"
	"os"
	"path/filepath"
	"testing"
)

func TestServerInterfaceHasAllOperationIDs(t *testing.T) {
	want := []string{
		"ListSlots",
		"GetSlot",
		"ListInstructors",
		"CreateBooking",
		"ListBookings",
		"GetBooking",
		"CancelBooking",
		"LeaveWaitlist",
		"JoinWaitlist",
		"GetWaitlistEntry",
		"DeleteWaitlistEntry",
		"GetProfile",
		"UpdateProfile",
		"RegisterPushToken",
		"CreateRating",
	}

	path := filepath.Join("api.gen.go")
	methods := interfaceMethods(t, path, "ServerInterface")
	for _, name := range want {
		if !methods[name] {
			t.Fatalf("ServerInterface missing method %s", name)
		}
	}
	if len(methods) != len(want) {
		t.Fatalf("ServerInterface has %d methods, want %d: %v", len(methods), len(want), methods)
	}
}

func interfaceMethods(t *testing.T, path, ifaceName string) map[string]bool {
	t.Helper()

	src, err := os.ReadFile(path)
	if err != nil {
		t.Fatalf("read file: %v", err)
	}
	fset := token.NewFileSet()
	f, err := parser.ParseFile(fset, path, src, 0)
	if err != nil {
		t.Fatalf("parse: %v", err)
	}

	out := make(map[string]bool)
	for _, decl := range f.Decls {
		gen, ok := decl.(*ast.GenDecl)
		if !ok || gen.Tok != token.TYPE {
			continue
		}
		for _, spec := range gen.Specs {
			ts, ok := spec.(*ast.TypeSpec)
			if !ok || ts.Name.Name != ifaceName {
				continue
			}
			iface, ok := ts.Type.(*ast.InterfaceType)
			if !ok {
				t.Fatalf("%s is not an interface", ifaceName)
			}
			for _, m := range iface.Methods.List {
				if len(m.Names) == 0 {
					continue
				}
				out[m.Names[0].Name] = true
			}
		}
	}
	return out
}
